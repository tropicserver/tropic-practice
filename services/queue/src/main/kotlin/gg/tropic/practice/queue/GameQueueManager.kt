package gg.tropic.practice.queue

import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.serializers.Serializers

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object GameQueueManager
{
    private val queues = mutableMapOf<String, GameQueue>()
    private val dpsQueueRedis = DPSRedisService("queue")
        .apply(DPSRedisService::start)

    private val dpsRedisCache = DPSRedisShared.keyValueCache

    fun queueSizeFromId(id: String) = dpsRedisCache
        .sync()
        .llen("tropicpractice:queues:$id:queue")

    fun popQueueEntryFromId(id: String) = dpsRedisCache
        .sync()
        .rpop("tropicpractice:queues:$id:queue")
        .let {
            Serializers.gson.fromJson(
                dpsRedisCache.sync()
                    .hget(
                        "tropicpractice:queues:$id:entries",
                        it
                    ),
                QueueEntry::class.java
            )
        }

    fun load()
    {
        KitDataSync.onReload {
            buildAndValidateQueueIndexes()
        }

        buildAndValidateQueueIndexes()

        dpsQueueRedis.configure {
            listen("join") {
                val entry = retrieve<QueueEntry>("entry")

                val kit = retrieve<String>("kit")
                val queueType = retrieve<QueueType>("queueType")
                val teamSize = retrieve<Int>("teamSize")

                val queueId = "$kit:${queueType.name}:${teamSize}v${teamSize}"

                queues[queueId]?.apply {
                    dpsRedisCache.sync().hset(
                        "tropicpractice:queues:$queueId:entries",
                        entry.leader.toString(),
                        Serializers.gson.toJson(entry)
                    )

                    dpsRedisCache.sync().lpush(
                        "tropicpractice:queues:$queueId:queue",
                        entry.leader.toString()
                    )

                    val queueState = QueueState(
                        kitId = kit,
                        queueType = queueType,
                        teamSize = teamSize,
                        joined = System.currentTimeMillis()
                    )
                    val jsonQueueState = Serializers.gson.toJson(queueState)

                    for (player in entry.players)
                    {
                        dpsRedisCache.sync().hset(
                            "tropicpractice:queue-states",
                            player.toString(),
                            jsonQueueState
                        )
                    }
                }
            }

            listen("leave") {
                val entry = retrieve<QueueEntry>("entry")

                val kit = retrieve<String>("kit")
                val queueType = retrieve<QueueType>("queueType")
                val teamSize = retrieve<Int>("teamSize")

                val queueId = "$kit:${queueType.name}:${teamSize}v${teamSize}"

                queues[queueId]?.apply {
                    dpsRedisCache.sync().lrem(
                        "tropicpractice:queues:$queueId:queue",
                        1,
                        entry.leader.toString()
                    )

                    dpsRedisCache.sync().hdel(
                        "tropicpractice:queues:$queueId:entries",
                        entry.leader.toString()
                    )

                    destroyQueueStates(entry)
                }
            }
        }
    }

    fun destroyQueueStates(entry: QueueEntry)
    {
        for (player in entry.players)
        {
            dpsRedisCache.sync().hdel(
                "tropicpractice:queue-states",
                player.toString()
            )
        }
    }

    private fun buildAndValidateQueueIndexes()
    {
        KitDataSync.cached().kits.values
            .forEach { kit ->
                QueueType.entries
                    .forEach scope@{
                        if (it == QueueType.Ranked && !kit.features(FeatureFlag.Ranked))
                        {
                            // a ranked queue exists for this kit, but the kit no longer supports ranked
                            val existingRanked = GameQueue(kit, it, 1)
                            if (queues.containsKey(existingRanked.queueId()))
                            {
                                queues
                                    .remove(existingRanked.queueId())
                                    ?.destroy()
                            }
                            return@scope
                        }

                        val queue = GameQueue(
                            kit = kit,
                            queueType = it,
                            teamSize = 1 // TODO: how do we do this?
                        )

                        if (!queues.containsKey(queue.queueId()))
                        {
                            queue.start()
                            queues[queue.queueId()] = queue
                        }
                    }
            }

        // cleanup queues for kits that no longer exist
        queues.toMap().forEach { (key, queue) ->
            val existingKit = KitDataSync
                .cached().kits[queue.kit.id]

            if (existingKit == null)
            {
                queues.remove(key)?.destroy()
            }
        }
    }
}
