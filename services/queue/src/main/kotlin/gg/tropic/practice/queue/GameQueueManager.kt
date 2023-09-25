package gg.tropic.practice.queue

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.DuelExpectation
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.map.ImmutableMap
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.replications.manager.ReplicationManager
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.CompletableFuture

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

    fun prepareGameFor(map: ImmutableMap, expectation: DuelExpectation, cleanup: () -> Unit): CompletableFuture<Void>
    {
        /**
         * At this point, we have a [DuelExpectation] that is saved in Redis, and
         * we've gotten rid of the queue entries from the list portion of queue. The players
         * still think they are in the queue, so we can generate the map and THEN update
         * their personal queue status. If they, for some reason, LEAVE the queue at this time, then FUCK ME!
         */
        // We need to synchronize this to prevent multiple games being allocated to the same map replication.
        synchronized(GameQueue.REPLICATION_LOCK_OBJECT) {
            val serverStatuses = ReplicationManager.allServerStatuses()

            // We're associating the server statuses by each server instead
            // of the map id which it is presented as.
            val serverToReplicationMappings = serverStatuses.values
                .flatMap {
                    it.replications.values.flatten()
                }
                .associateBy {
                    it.server
                }

            val availableReplication = serverToReplicationMappings.values
                .firstOrNull {
                    !it.inUse && it.associatedMapName == map.name
                }

            val serverToRequestReplication = ServerContainer
                .getServersInGroupCasted<GameServer>("mipgame")
                .sortedBy(GameServer::getPlayersCount)
                .firstOrNull()
                ?.id
                ?: return run {
                    DPSRedisShared.sendMessage(
                        expectation.players,
                        listOf(
                            "&c&lError: &cWe found no game server available to house your game!"
                        )
                    )

                    CompletableFuture.completedFuture(null)
                }

            val replication = if (availableReplication == null)
            {
                ReplicationManager.requestReplication(
                    serverToRequestReplication, map.name, expectation.identifier
                )
            } else
            {
                ReplicationManager.allocateReplication(
                    serverToRequestReplication, map.name, expectation.identifier
                )
            }

            return replication.thenAcceptAsync {
                if (it.status == ReplicationManager.ReplicationResultStatus.Completed)
                {
                    Thread.sleep(100L)
                    DPSRedisShared.redirect(
                        expectation.players, serverToRequestReplication
                    )
                }

                if (it.status == ReplicationManager.ReplicationResultStatus.Unavailable)
                {
                    DPSRedisShared.sendMessage(
                        expectation.players,
                        listOf(
                            "&c&lError: &cWe weren't able to allocate a map for you!",
                            "&c&lReason: &f${it.message ?: "???"}"
                        )
                    )
                }

                cleanup()
            }.exceptionally {
                DPSRedisShared.sendMessage(
                    expectation.players,
                    listOf(
                        "&c&lError: &cWe weren't able to allocate a map for you!",
                        "&c&lReason: &f${it.message ?: "???"}" // TODO: better message?
                    )
                )
                return@exceptionally null
            }
        }
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

                    destroyQueueStates(queueId(), entry)
                }
            }
        }
    }

    fun destroyQueueStates(queueID: String, entry: QueueEntry)
    {
        dpsRedisCache.sync().hdel(
            "tropicpractice:queues:$queueID:entries",
            entry.leader.toString()
        )

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
