package gg.tropic.practice.queue

import gg.tropic.practice.PracticeShared
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.namespace
import gg.tropic.practice.region.Region
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.serializers.Serializers
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
class GameQueue(
    val kit: ImmutableKit,
    private val queueType: QueueType,
    private val teamSize: Int
) : () -> Unit
{
    companion object
    {
        @JvmStatic
        val RUN_RANGE_EXPANSION_UPDATES = { time: Long ->
            System.currentTimeMillis() >= time + 1500L
        }
    }

    private var thread: Thread? = null
    private var periodicUpdateThread: Thread? = null
    private val metadataUpdateThread = Executors
        .newSingleThreadScheduledExecutor()

    fun queueId() = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"

    private fun expandRanges()
    {
        val entries = GameQueueManager.getQueueEntriesFromId(queueId())

        for ((hash, entry) in entries)
        {
            var requiresUpdates = false

            if (queueType == QueueType.Ranked)
            {
                if (RUN_RANGE_EXPANSION_UPDATES(entry.lastELORangeExpansion))
                {
                    entry.leaderRangedELO.diffsBy = min(
                        2_000, // TODO is 2000 going to be the max ELO?
                        (entry.leaderRangedELO.diffsBy * 1.5).toInt()
                    )
                    entry.lastELORangeExpansion = System.currentTimeMillis()
                    requiresUpdates = true
                }
            }

            val previousPingDiff = entry.leaderRangedPing.diffsBy
            if (entry.maxPingDiff != -1)
            {
                if (entry.leaderRangedPing.diffsBy < entry.maxPingDiff)
                {
                    if (RUN_RANGE_EXPANSION_UPDATES(entry.lastPingRangeExpansion))
                    {
                        entry.leaderRangedPing.diffsBy = min(
                            entry.maxPingDiff,
                            (entry.leaderRangedPing.diffsBy * 1.5).toInt()
                        )
                        entry.lastPingRangeExpansion = System.currentTimeMillis()
                        val differential = entry.leaderRangedPing.diffsBy - previousPingDiff
                        entry.lastRecordedDifferential = differential

                        requiresUpdates = true

                        val pingRange = entry.leaderRangedPing.toIntRangeInclusive()
                        DPSRedisShared.sendMessage(
                            entry.players,
                            listOf(
                                "{secondary}You are matchmaking in an ping range of ${
                                    "&a[${max(0, pingRange.first)} -> ${pingRange.last}]{secondary}"
                                } &7(expanded by ±${
                                    differential
                                }). ${
                                    if (entry.leaderRangedPing.diffsBy == entry.maxPingDiff) "&lThe range will no longer be expanded as it has reached its maximum of ±${entry.maxPingDiff}!" else ""
                                }"
                            )
                        )
                    }
                }
            }

            if (
                entry.preferredQueueRegion != Region.Both &&
                System.currentTimeMillis() - entry.joinQueueTimestamp >= 10_000L
            )
            {
                requiresUpdates = true

                val previousRegion = entry.preferredQueueRegion
                entry.preferredQueueRegion = Region.Both

                DPSRedisShared.sendMessage(
                    entry.players,
                    listOf(
                        "{secondary}You are now matchmaking in the &aGlobal{secondary} queue as we could not find an opponent for you in the {primary}${previousRegion.name}{secondary} queue."
                    )
                )
            }

            if (requiresUpdates)
            {
                GameQueueManager.useCache {
                    hset(
                        "${namespace().suffixWhenDev()}:queues:${queueId()}:entries",
                        hash, Serializers.gson.toJson(entry)
                    )
                }
            }
        }
    }

    fun expandRangeBlock()
    {
        while (true)
        {
            runCatching {
                expandRanges()
            }.onFailure {
                it.printStackTrace()
            }

            Thread.sleep(1000L)
        }
    }

    fun start()
    {
        check(thread == null)
        Logger.getGlobal()
            .info(
                "[queue] Building a queue with id: ${queueId()}"
            )

        thread = thread(
            isDaemon = true,
            name = "queues-${queueId()}",
            block = this
        )

        Logger.getGlobal()
            .info(
                "[queue] Started queue iterator"
            )

        check(periodicUpdateThread == null)
        periodicUpdateThread = thread(
            isDaemon = true,
            name = "queue-periodic-${queueId()}",
            block = ::expandRangeBlock
        )

        Logger.getGlobal()
            .info(
                "[queue] Started adjacent periodic thread"
            )

        metadataUpdateThread.scheduleAtFixedRate({
            with(DPSRedisShared.keyValueCache.sync()) {
                val queueSize = GameQueueManager.queueSizeFromId(queueId())
                psetex("${namespace().suffixWhenDev()}:metadata:users-queued:${
                    queueId()
                }", 1000L, queueSize.toString())

                val gamesMatchingQueueID = GameManager.allGames()
                    .thenApply { refs ->
                        refs.filter { it.queueId == queueId() }
                    }
                    .join()
                val playersInGame = gamesMatchingQueueID.sumOf { it.players.size }

                psetex("${namespace().suffixWhenDev()}:metadata:users-playing:${
                    queueId()
                }", 1000L, playersInGame.toString())
            }
        }, 0L, 50L, TimeUnit.MILLISECONDS)

        metadataUpdateThread.scheduleAtFixedRate({
            kotlin.runCatching {
                GameQueueManager.getAllQueuePlayers(queueId())
                    .forEach {
                        val entry = GameQueueManager.getQueueEntryFromId(queueId(), it)
                        if (entry != null)
                        {
                            return@forEach
                        }

                        GameQueueManager
                            .removeQueueEntryFromId(queueId(), UUID.fromString(it))

                        Logger.getGlobal().info("Removing $it as the player does not have a queue entry")
                    }

                GameQueueManager.getQueueEntriesFromId(queueId())
                    .forEach { (t, u) ->
                        if (System.currentTimeMillis() - u.joinQueueTimestamp >= 60_000L)
                        {
                            val isPlayerOnline = DPSRedisShared.keyValueCache.sync()
                                .hexists(
                                    "player:${u.leader}",
                                    "instance"
                                )

                            if (!isPlayerOnline)
                            {
                                GameQueueManager.removeQueueEntryFromId(queueId(), u.leader)
                            }
                        }
                    }
            }.onFailure {
                it.printStackTrace()
            }
        }, 0L, 1L, TimeUnit.SECONDS)

        Logger.getGlobal()
            .info(
                "[queue] Built queue with ID: ${queueId()}"
            )
    }

    fun cleanup()
    {
        GameQueueManager
            .getQueueEntriesFromId(queueId())
            .forEach { (_, value) ->
                GameQueueManager.removeQueueEntryFromId(queueId(), value.leader)
                GameQueueManager.destroyQueueStates(
                    queueID = queueId(),
                    entry = value
                )

                DPSRedisShared.sendMessage(
                    value.players,
                    listOf(
                        "&c&lDue to a reboot of one of our systems, you have been removed from the queue you were in."
                    )
                )
            }

        Logger.getGlobal()
            .info(
                "[queue] Cleanup procedure for queue ${queueId()} completed."
            )
    }

    fun destroy()
    {
        checkNotNull(thread)
        thread?.interrupt()
        thread = null

        if (periodicUpdateThread != null)
        {
            periodicUpdateThread?.interrupt()
            periodicUpdateThread = null
        }

        metadataUpdateThread.shutdownNow()
    }

    private fun run()
    {
        val length = GameQueueManager.queueSizeFromId(queueId())

        if (length < teamSize * 2)
        {
            Thread.sleep(200)
            return
        }

        // don't unnecessarily load in and map to data class if not needed
        val first: List<QueueEntry>
        val second: List<QueueEntry>

        // Faster than doing a list intersect which compares all items
        fun IntRange.quickIntersect(other: IntRange): Boolean
        {
            return this.first <= other.last && this.last >= other.first
        }

        val queueEntries = GameQueueManager.getQueueEntriesFromId(queueId())
        val groupedQueueEntries = if (queueType == QueueType.Ranked)
        {
            queueEntries.values
                .map { entry ->
                    val otherEntriesMatchingEntry = queueEntries.values
                        .filter { otherEntry ->
                            val doesELOIntersect = entry.leaderRangedELO.toIntRangeInclusive()
                                .quickIntersect(
                                    otherEntry.leaderRangedELO
                                        .toIntRangeInclusive()
                                )

                            // we can ignore ping intersections if they have no ping restriction
                            val doesPingIntersect = (entry.maxPingDiff == -1 || otherEntry.maxPingDiff == -1) ||
                                entry.leaderRangedPing.toIntRangeInclusive()
                                    .quickIntersect(
                                        otherEntry.leaderRangedPing
                                            .toIntRangeInclusive()
                                    )

                            entry != otherEntry &&
                                doesELOIntersect &&
                                doesPingIntersect &&
                                ((entry.queueRegion == otherEntry.queueRegion) ||
                                    (entry.preferredQueueRegion == otherEntry.preferredQueueRegion))
                        }

                    otherEntriesMatchingEntry + entry
                }
                .filter {
                    it.size >= teamSize * 2
                }
        } else
        {
           queueEntries.values
                .map { entry ->
                    val otherEntriesMatchingEntry = queueEntries.values
                        .filter { otherEntry ->
                            // we can ignore ping intersections if they have no ping restriction
                            val doesPingIntersect = (entry.maxPingDiff == -1 || otherEntry.maxPingDiff == -1) ||
                                entry.leaderRangedPing.toIntRangeInclusive()
                                    .quickIntersect(
                                        otherEntry.leaderRangedPing
                                            .toIntRangeInclusive()
                                    )

                            entry != otherEntry &&
                                ((entry.queueRegion == otherEntry.queueRegion) ||
                                    (entry.preferredQueueRegion == otherEntry.preferredQueueRegion)) &&
                                doesPingIntersect
                        }

                    otherEntriesMatchingEntry + entry
                }
                .filter {
                    it.size >= teamSize * 2
                }
        }

        if (groupedQueueEntries.isEmpty())
        {
            Thread.sleep(200)
            return
        }

        val group = groupedQueueEntries.random()
            .onEach {
                runCatching {
                    GameQueueManager.removeQueueEntryFromId(queueId(), it.leader)
                }.onFailure {
                    it.printStackTrace()
                }
            }

        // Expecting a symmetric list here, lets hope it doesn't break?
        first = group.take(teamSize)
        second = group.takeLast(teamSize)

        if (first.size != teamSize || second.size != teamSize)
        {
            Thread.sleep(200)
            return
        }

        val firstPlayers = first.flatMap { it.players }
        val secondPlayers = second.flatMap { it.players }

        val users = listOf(
            firstPlayers,
            secondPlayers
        ).flatten()

        val map = MapDataSync
            .selectRandomMapCompatibleWith(kit)
            ?: return run {
                DPSRedisShared.sendMessage(
                    users,
                    listOf(
                        "&c&lWe found no map compatible with the kit you are queueing for!"
                    )
                )
            }

        val expectation = GameExpectation(
            identifier = UUID.randomUUID(),
            players = listOf(first, second)
                .flatMap {
                    it.flatMap { entry ->
                        entry.players
                    }
                },
            teams = mapOf(
                GameTeamSide.A to GameTeam(side = GameTeamSide.A, players = firstPlayers),
                GameTeamSide.B to GameTeam(side = GameTeamSide.B, players = secondPlayers),
            ),
            kitId = kit.id,
            mapId = map.name,
            queueType = queueType,
            queueId = queueId()
        )

        val region = first.first().preferredQueueRegion
        GameQueueManager.prepareGameFor(
            map = map,
            expectation = expectation,
            // prefer NA servers if queuing globally
            region = if (region == Region.Both) Region.NA else region,
            cleanup = {
                for (queueEntry in first + second)
                {
                    GameQueueManager
                        .destroyQueueStates(
                            queueId(), queueEntry
                        )
                }
            }
        ).exceptionally {
            it.printStackTrace()
            return@exceptionally null
        }.join()

        Thread.sleep(200)
    }

    override fun invoke()
    {
        while (true)
        {
            runCatching {
                run()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
