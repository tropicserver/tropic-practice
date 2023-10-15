package gg.tropic.practice.queue

import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.QueueType
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.thread

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
        val REPLICATION_LOCK_OBJECT = Any()
    }

    private var thread: Thread? = null

    fun queueId() = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"

    fun start()
    {
        check(thread == null)
        thread = thread(
            isDaemon = true,
            name = "queues-${queueId()}",
            block = this
        )

        Logger.getGlobal()
            .info(
                "Building a queue with id: ${queueId()}"
            )
    }

    fun destroy()
    {
        checkNotNull(thread)
        thread?.interrupt()
        thread = null
    }

    private fun run()
    {
        val length = GameQueueManager
            .queueSizeFromId(queueId())

        if (length < 2)
        {
            Thread.sleep(200)
            return
        }

        val first = GameQueueManager.popQueueEntryFromId(queueId(), teamSize)
        val second = GameQueueManager.popQueueEntryFromId(queueId(), teamSize)

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
