package gg.tropic.practice.queue

import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.application.api.defaults.game.DuelExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.replications.manager.ReplicationManager
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

    override fun invoke()
    {
        while (true)
        {
            val length = GameQueueManager
                .queueSizeFromId(queueId())

            if (length < 2)
            {
                Thread.sleep(1000)
                continue
            }

            val first = GameQueueManager
                .popQueueEntryFromId(queueId())
            val second = GameQueueManager
                .popQueueEntryFromId(queueId())

            // TODO: give feedback if it's not gonna actually send them into a game
            val map = MapDataSync
                .selectRandomMapCompatibleWith(kit)
                ?: continue

            val expectation = DuelExpectation(
                identifier = UUID.randomUUID(),
                players = listOf(first, second).flatMap { it.players },
                teams = mapOf(
                    GameTeamSide.A to GameTeam(side = GameTeamSide.A, players = first.players),
                    GameTeamSide.B to GameTeam(side = GameTeamSide.B, players = second.players),
                ),
                kitId = kit.id,
                mapId = map.name
            )

            DataStoreObjectControllerCache
                .findNotNull<DuelExpectation>()
                .save(expectation, DataStoreStorageType.REDIS)
                .join()

            /**
             * Ok. At this point, we have a [DuelExpectation] that is saved in Redis, and
             * we've gotten rid of the queue entries from the list portion of queue. The players
             * still think they are in the queue, so we can generate the map and THEN update
             * their personal queue status. If they, for some reason, LEAVE the queue at this time, then FUCK ME!
             */
            // We need to synchronize this to prevent multiple games being allocated to the same map replication.
            synchronized(REPLICATION_LOCK_OBJECT) {
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

                // TODO: we're !ASSUMING! one of these servers are available lol
                // TODO: Do some type of load balancing here? WTF?
                val serverToRequestReplication = serverStatuses.keys.random()
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

                replication.thenAccept {
                    if (it == ReplicationManager.ReplicationResult.Completed)
                    {
                        ReplicationManager.sendPlayersToServer(
                            listOf(first.players, second.players).flatten(),
                            serverToRequestReplication
                        )
                    } else
                    {
                        // TODO: do they even know? LOL feedback pls
                        GameQueueManager.destroyQueueStates(first)
                        GameQueueManager.destroyQueueStates(second)
                    }
                }
            }
        }
    }
}
