package gg.tropic.practice.queue

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.map.ImmutableMap
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.replications.manager.ReplicationManager
import io.lettuce.core.api.sync.RedisCommands
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID
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

    fun useCache(
        block: RedisCommands<String, String>.() -> Unit
    )
    {
        block(dpsRedisCache.sync())
    }

    fun queueSizeFromId(id: String) = dpsRedisCache
        .sync()
        .llen("tropicpractice:queues:$id:queue")

    fun getQueueEntriesFromId(id: String) = dpsRedisCache.sync()
        .hgetall("tropicpractice:queues:$id:entries")
        .mapValues {
            Serializers.gson.fromJson(
                it.value,
                QueueEntry::class.java
            )
        }

    fun popQueueEntryFromId(id: String, amount: Int) = dpsRedisCache
        .sync()
        .lpop("tropicpractice:queues:$id:queue", amount.toLong())
        .map {
            Serializers.gson.fromJson(
                dpsRedisCache.sync()
                    .hget(
                        "tropicpractice:queues:$id:entries",
                        it
                    ),
                QueueEntry::class.java
            )
        }

    fun prepareGameFor(map: ImmutableMap, expectation: GameExpectation, cleanup: () -> Unit): CompletableFuture<Void>
    {
        val distinctUsers = expectation.players.distinct()
        if (distinctUsers.size != expectation.players.size)
        {
            DPSRedisShared.sendMessage(
                expectation.players,
                listOf(
                    "&c&lError: &cAn issue occurred when creating your game! (duplicate players on teams)"
                )
            )

            return CompletableFuture.runAsync {
                cleanup()
            }
        }

        /**
         * Although we check for the map lock when searching for a random map,
         * we want to handle this edge case for duels and anything else.
         */
        if (map.locked)
        {
            DPSRedisShared.sendMessage(
                expectation.players,
                listOf(
                    "&c&lError: &cThe map you were allocated to play a game on is locked!"
                )
            )

            return CompletableFuture.runAsync {
                cleanup()
            }
        }

        /**
         * At this point, we have a [GameExpectation] that is saved in Redis, and
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

                    CompletableFuture.runAsync {
                        cleanup()
                    }
                }

            val replication = if (availableReplication == null)
            {
                ReplicationManager.requestReplication(
                    serverToRequestReplication, map.name, expectation
                )
            } else
            {
                ReplicationManager.allocateReplication(
                    serverToRequestReplication, map.name, expectation
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
                            "&c&lError: &cWe weren't able to allocate a map for you! (${it.message ?: "???"})",
                        )
                    )
                }

                cleanup()
            }.exceptionally {
                DPSRedisShared.sendMessage(
                    expectation.players,
                    listOf(
                        "&c&lError: &cWe weren't able to allocate a map for you! (${it.message ?: "???"})"
                    )
                )

                cleanup()
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
                val leader = retrieve<UUID>("leader")
                val queueId = retrieve<String>("queueID")

                queues[queueId]?.apply {
                    dpsRedisCache.sync().lrem(
                        "tropicpractice:queues:$queueId:queue",
                        1,
                        leader.toString()
                    )

                    val queueEntry = Serializers
                        .gson.fromJson(
                            dpsRedisCache.sync().hget(
                                "tropicpractice:queues:$queueId:entries",
                                leader.toString()
                            ),
                            QueueEntry::class.java
                        )

                    dpsRedisCache.sync().hdel(
                        "tropicpractice:queues:$queueId:entries",
                        leader.toString()
                    )

                    destroyQueueStates(queueId(), queueEntry)
                }
            }
        }
    }

    fun destroyQueueStates(queueID: String, entry: QueueEntry)
    {
        runCatching {
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
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun buildAndValidateQueueIndexes()
    {
        KitDataSync.cached().kits.values
            .forEach { kit ->
                val sizeModels = kit
                    .featureConfig(
                        FeatureFlag.QueueSizes,
                        key = "sizes"
                    )
                    .split(",")
                    .map { sizeModel ->
                        val split = sizeModel.split(":")
                        split[0].toInt() to (split.getOrNull(1)
                            ?.split("+")
                            ?.map(QueueType::valueOf)
                            ?: listOf(QueueType.Casual))
                    }

                QueueType.entries
                    .forEach scope@{
                        for (model in sizeModels)
                        {
                            if (
                                it == QueueType.Ranked &&
                                (
                                    !kit.features(FeatureFlag.Ranked) ||
                                        QueueType.Ranked !in model.second
                                    )
                            )
                            {
                                // a ranked queue exists for this kit, but the kit no longer supports ranked
                                val existingRanked = GameQueue(kit, it, model.first)
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
                                teamSize = model.first
                            )

                            if (!queues.containsKey(queue.queueId()))
                            {
                                queue.start()
                                queues[queue.queueId()] = queue
                            }
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
