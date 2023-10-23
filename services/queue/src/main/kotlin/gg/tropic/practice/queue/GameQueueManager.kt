package gg.tropic.practice.queue

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.map.ImmutableMap
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.DuelRequest
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.games.SpectateRequest
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.games.models.GameReference
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.replications.manager.ReplicationManager
import gg.tropic.practice.serializable.Message
import io.lettuce.core.api.sync.RedisCommands
import net.evilblock.cubed.serializers.Serializers
import net.md_5.bungee.api.chat.ClickEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object GameQueueManager
{
    private val queues = mutableMapOf<String, GameQueue>()
    private val dpsQueueRedis = DPSRedisService("queue")
        .apply(DPSRedisService::start)

    private val replicationAllocator = Executors.newSingleThreadScheduledExecutor()
        .apply {
            Runtime.getRuntime().addShutdownHook(Thread {
                println("Waiting for terminating of replication allocator")
                awaitTermination(5L, TimeUnit.SECONDS)
            })
        }
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

    fun removeQueueEntryFromId(id: String, entry: UUID) = dpsRedisCache
        .sync()
        .lrem("tropicpractice:queues:$id:queue", 1, entry.toString())

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
                    "&cAn issue occurred when creating your game! (duplicate players on teams)"
                )
            )

            return CompletableFuture.runAsync({ cleanup() }, replicationAllocator)
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
                    "&cThe map you were allocated to play a game on is locked!"
                )
            )

            return CompletableFuture.runAsync({ cleanup() }, replicationAllocator)
        }

        /**
         * At this point, we have a [GameExpectation] that is saved in Redis, and
         * we've gotten rid of the queue entries from the list portion of queue. The players
         * still think they are in the queue, so we can generate the map and THEN update
         * their personal queue status. If they, for some reason, LEAVE the queue at this time, then FUCK ME!
         */
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
                        "&cWe found no game server available to house your game!"
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

        return replication
            .thenAcceptAsync({
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
                            "&cWe weren't able to allocate a map for you! (${it.message ?: "???"})",
                        )
                    )
                }

                cleanup()
            }, replicationAllocator)
            .exceptionally {
                DPSRedisShared.sendMessage(
                    expectation.players,
                    listOf(
                        "&cWe weren't able to allocate a map for you! (${it.message ?: "???"})"
                    )
                )

                cleanup()
                return@exceptionally null
            }
    }

    data class SpectateResponse(val status: SpectateResult, val message: String)

    enum class SpectateResult
    {
        Complete, Unavailable
    }

    private val spectateCallbacks = Caffeine
        .newBuilder()
        .removalListener<UUID, CompletableFuture<SpectateResponse>> { _, value, cause ->
            if (cause == RemovalCause.EXPIRED)
            {
                value?.complete(
                    SpectateResponse(
                        status = SpectateResult.Unavailable,
                        message = "We weren't able to put you as a spectator in the game!"
                    )
                )
            }
        }
        .expireAfterWrite(5L, TimeUnit.SECONDS)
        .build<UUID, CompletableFuture<SpectateResponse>>()

    fun requestSpectate(
        server: String,
        request: SpectateRequest,
        game: GameReference
    ): CompletableFuture<SpectateResponse>
    {
        val future = CompletableFuture<SpectateResponse>()
        val requestID = UUID.randomUUID()
        dpsQueueRedis.createMessage(
            "request-spectate",
            "requestID" to requestID,
            "request" to request,
            "server" to server,
            "game" to game.uniqueId
        ).publish(
            AwareThreadContext.SYNC,
            channel = "practice:queue-inhabitants"
        )

        spectateCallbacks.put(requestID, future)
        return future
    }

    fun playerIsOnline(uniqueId: UUID) = dpsRedisCache.sync()
        .hexists("player:$uniqueId", "server")

    fun playerIsQueued(uniqueId: UUID) = dpsRedisCache.sync()
        .hexists("tropicpractice:queue-states", uniqueId.toString())

    fun load()
    {
        KitDataSync.onReload {
            buildAndValidateQueueIndexes()
        }

        buildAndValidateQueueIndexes()
        dpsRedisCache.sync().del("tropicpractice:duelrequests:*")

        Logger.getGlobal().info("Invalidated existing duel requests")

        val executor = Executors.newSingleThreadScheduledExecutor()
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Terminating all duel request invalidators before shutdown")
            executor.shutdownNow()
        })

        dpsQueueRedis.configure {
            listen("spectate-ready") {
                val requestID = retrieve<UUID>("requestID")

                spectateCallbacks.getIfPresent(requestID)
                    ?.complete(
                        SpectateResponse(SpectateResult.Complete, "")
                    )
            }

            val futureMappings = mutableMapOf<String, ScheduledFuture<*>>()
            listen("accept-duel") {
                val request = retrieve<DuelRequest>("request")
                val key = "tropicpractice:duelrequests:${request.requester}:${request.kitID}"
                futureMappings[key]?.cancel(true)
                dpsRedisCache.sync().hdel(key, request.requestee.toString())

                // TODO: ensure player is still on lobby
                if (!playerIsOnline(request.requester))
                {
                    DPSRedisShared.sendMessage(
                        listOf(request.requestee),
                        listOf("&cThe player that sent you the duel request is no longer online!")
                    )
                    return@listen
                }

                if (playerIsQueued(request.requester))
                {
                    DPSRedisShared.sendMessage(
                        listOf(request.requestee),
                        listOf("&cThe player that sent you the duel request is currently queued for a game!")
                    )
                    return@listen
                }

                GameManager.allGames()
                    .thenApply {
                        it.firstOrNull { ref -> request.requester in ref.players }
                    }
                    .thenAccept {
                        if (it != null)
                        {
                            DPSRedisShared.sendMessage(
                                listOf(request.requestee),
                                listOf("&cThe player that sent you the duel request is currently in a game!")
                            )
                            return@thenAccept
                        }

                        val kit = KitDataSync.cached().kits[request.kitID]
                            ?: return@thenAccept run {
                                DPSRedisShared.sendMessage(
                                    listOf(request.requestee),
                                    listOf(
                                        "&cThe kit you received a duel request for no longer exists!"
                                    )
                                )
                            }

                        // we need to do the check again, so why not
                        val map = if (request.mapID == null)
                        {
                            MapDataSync
                                .selectRandomMapCompatibleWith(kit)
                        } else
                        {
                            MapDataSync.cached().maps[request.mapID]
                        } ?: return@thenAccept run {
                            DPSRedisShared.sendMessage(
                                listOf(request.requestee),
                                listOf(
                                    "&cWe found no map compatible with the kit you received a duel request for!"
                                )
                            )
                        }

                        prepareGameFor(map, GameExpectation(
                            players = listOf(request.requester, request.requestee),
                            identifier = UUID.randomUUID(),
                            teams = mapOf(
                                GameTeamSide.A to GameTeam(GameTeamSide.A, listOf(request.requester)),
                                GameTeamSide.B to GameTeam(GameTeamSide.B, listOf(request.requestee))
                            ),
                            kitId = request.kitID,
                            mapId = map.name
                        )) {
                            println("[debug] prepared duel game for ${request.requester}")
                        }
                    }
            }

            listen("request-duel") {
                val request = retrieve<DuelRequest>("request")
                val key = "tropicpractice:duelrequests:${request.requester}:${request.kitID}"
                dpsRedisCache.sync().hset(
                    key,
                    request.requestee.toString(),
                    Serializers.gson.toJson(request)
                )

                val kit = KitDataSync.cached().kits[request.kitID]!!
                val map = if (request.mapID != null)
                {
                    MapDataSync.cached().maps[request.mapID]
                } else null

                val requesterName = ScalaStoreUuidCache.username(request.requester)
                DPSRedisShared.sendMessage(
                    listOf(request.requestee),
                    Message()
                        .withMessage(
                            " ",
                            "&6Duel request:",
                            "&7From: &f$requesterName",
                            "&7Kit: &f${kit.displayName}",
                            "&7Map: &f${map?.displayName ?: "Random"}",
                            "",
                            ""
                        )
                        .withMessage(
                            "&a(Click to accept)"
                        )
                        .andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/accept $requesterName ${kit.id}"
                        )
                        .andHoverOf("Click to accept!")
                        .withMessage("")
                )

                futureMappings[key] = executor.schedule({
                    DPSRedisShared.sendMessage(
                        listOf(request.requestee),
                        listOf("&cYour duel request from &f${requesterName}&c with kit &f${kit.displayName}&c has expired!")
                    )

                    dpsRedisCache.sync().hdel(key, request.requestee.toString())
                }, 1L, TimeUnit.MINUTES)
            }

            listen("spectate") {
                val request = retrieve<SpectateRequest>("request")

                GameManager.allGames()
                    .thenApply {
                        it.firstOrNull { ref -> request.target in ref.players }
                    }
                    .thenCompose {
                        if (it == null)
                        {
                            DPSRedisShared.sendMessage(
                                listOf(request.player),
                                listOf("&cThe player you tried to spectate is not in a game!")
                            )
                            return@thenCompose CompletableFuture
                                .completedFuture(null)
                        }

                        if (!it.majorityAllowsSpectators && !request.bypassesSpectatorAllowanceChecks)
                        {
                            DPSRedisShared.sendMessage(
                                listOf(request.player),
                                listOf("&cThe game you tried to spectate has spectators disabled!")
                            )
                            return@thenCompose CompletableFuture
                                .completedFuture(null)
                        }

                        requestSpectate(it.server, request, it)
                            .thenApply { resp ->
                                resp to it.server
                            }
                    }
                    .thenApply {
                        if (it?.first?.status == SpectateResult.Complete)
                        {
                            DPSRedisShared.redirect(
                                listOf(request.player),
                                it.second
                            )
                        } else
                        {
                            DPSRedisShared.sendMessage(
                                listOf(request.player),
                                listOf("&c${it?.second ?: "The game you tried to join is unavailable for spectators!"}")
                            )
                        }
                    }
            }

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
                                runCatching {
                                    queue.cleanup()
                                }.onFailure { failure ->
                                    failure.printStackTrace()
                                }

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
