package gg.tropic.practice.queue

import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.QueueType
import net.evilblock.cubed.serializers.Serializers
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

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
    private var adjacentRankedThread: Thread? = null

    fun queueId() = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"

    private fun expandRanges()
    {
        val entries = GameQueueManager.getQueueEntriesFromId(queueId())

        for ((hash, entry) in entries)
        {
            var requiresUpdates = false

            if (RUN_RANGE_EXPANSION_UPDATES(entry.lastELORangeExpansion))
            {
                entry.leaderRangedELO.diffsBy = min(
                    2_000, // TODO is 2000 going to be the max ELO?
                    (entry.leaderRangedELO.diffsBy * 1.5).toInt()
                )
                entry.lastELORangeExpansion = System.currentTimeMillis()
                requiresUpdates = true
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
                        requiresUpdates = true

                        val pingRange = entry.leaderRangedPing.toIntRangeInclusive()
                        DPSRedisShared.sendMessage(
                            entry.players,
                            listOf(
                                "{secondary}You are matchmaking in an ping range of ${
                                    "&a[${max(0, pingRange.first)} -> ${pingRange.last}]{secondary}"
                                } &7(expanded by ±${
                                    entry.leaderRangedPing.diffsBy - previousPingDiff
                                }). ${
                                    if (entry.leaderRangedPing.diffsBy == entry.maxPingDiff) "&lThe range will no longer be expanded as it has reached its maximum of ±${entry.maxPingDiff}!" else ""
                                }"
                            )
                        )
                    }
                }
            }

            if (requiresUpdates)
            {
                GameQueueManager.useCache {
                    hset(
                        "tropicpractice:queues:${queueId()}:entries",
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
        thread = thread(
            isDaemon = true,
            name = "queues-${queueId()}",
            block = this
        )

        if (queueType == QueueType.Ranked)
        {
            check(adjacentRankedThread == null)
            adjacentRankedThread = thread(
                isDaemon = true,
                name = "queue-rangeexpander-${queueId()}",
                block = ::expandRangeBlock
            )
        }

        Logger.getGlobal()
            .info(
                "Building a queue with id: ${queueId()}"
            )
    }

    fun cleanup()
    {
        GameQueueManager.getQueueEntriesFromId(queueId())
            .forEach { (_, value) ->
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
                "Cleanup procedure for queue ${queueId()} completed."
            )
    }

    fun destroy()
    {
        checkNotNull(thread)
        thread?.interrupt()
        thread = null

        if (adjacentRankedThread != null)
        {
            adjacentRankedThread?.interrupt()
            adjacentRankedThread = null
        }
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

        if (queueType == QueueType.Ranked)
        {
            // Faster than doing a list intersect which compares all items
            fun IntRange.quickIntersect(other: IntRange): Boolean
            {
                return this.first <= other.last && this.last >= other.first
            }

            val queueEntries = GameQueueManager.getQueueEntriesFromId(queueId())
            val groupedQueueEntries = queueEntries.values
                .map { entry ->
                    val otherEntriesMatchingEntry = queueEntries.values
                        .filter { otherEntry ->
                            val doesELOIntersect = entry.leaderRangedELO.toIntRangeInclusive()
                                .quickIntersect(
                                    otherEntry.leaderRangedELO
                                        .toIntRangeInclusive()
                                )

                            // we can ignore ping intersections if they have no ping restriction
                            val doesPingIntersect = entry.maxPingDiff == -1 ||
                                entry.leaderRangedPing.toIntRangeInclusive()
                                    .quickIntersect(
                                        otherEntry.leaderRangedPing
                                            .toIntRangeInclusive()
                                    )

                            entry != otherEntry && doesELOIntersect && doesPingIntersect
                        }

                    otherEntriesMatchingEntry + entry
                }
                .filter {
                    it.size >= teamSize * 2
                }

            for ((index, groupedQueueEntry) in groupedQueueEntries.withIndex())
            {
                println("[debug] [$index] New group with ${groupedQueueEntry.size}")
                for (queueEntry in groupedQueueEntry)
                {
                    val pingRange = queueEntry.leaderRangedPing.toIntRangeInclusive()
                    val eloRange = queueEntry.leaderRangedELO.toIntRangeInclusive()
                    println("[debug]     - ${queueEntry.leader}")
                    println("[debug]       | ping range: [${pingRange.first} -> ${pingRange.last}]")
                    println("[debug]       | ELO range: [${eloRange.first} -> ${eloRange.last}]")
                }
            }

            if (groupedQueueEntries.isEmpty())
            {
                Thread.sleep(200)
                return
            }

            val group = groupedQueueEntries.random()
                .onEach {
                    GameQueueManager.removeQueueEntryFromId(queueId(), it.leader)
                }

            // Expecting a symmetric list here, lets hope it doesn't break?
            first = group.take(teamSize)
            second = group.takeLast(teamSize)
        } else
        {
            first = GameQueueManager.popQueueEntryFromId(queueId(), teamSize)
            second = GameQueueManager.popQueueEntryFromId(queueId(), teamSize)
        }

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
                        "&c&lError: &cWe found no map compatible with the kit you are queueing for!"
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
            mapId = map.name
        )

        GameQueueManager.prepareGameFor(
            map = map,
            expectation = expectation
        ) {
            for (queueEntry in first + second)
            {
                GameQueueManager
                    .destroyQueueStates(
                        queueId(), queueEntry
                    )
            }
        }
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
