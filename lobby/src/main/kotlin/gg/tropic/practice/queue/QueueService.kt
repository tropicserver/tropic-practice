package gg.tropic.practice.queue

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.category.eloRange
import gg.tropic.practice.category.pingRange
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.profile.PracticeProfileService
import net.evilblock.cubed.util.nms.MinecraftReflection
import org.bukkit.entity.Player
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@Service
object QueueService
{
    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:queue")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    @Configure
    fun configure()
    {
        this.aware.connect()
            .toCompletableFuture()
            .join()
    }

    fun leaveQueue(player: Player)
    {
        val lobbyPlayer = LobbyPlayerService
            .find(player.uniqueId)
            ?: return

        val queueID = lobbyPlayer.buildQueueID()
            ?: return

        createMessage(
            packet = "leave",
            "leader" to player.uniqueId,
            "queueID" to queueID
        ).publish(
            context = AwareThreadContext.SYNC
        )

        // set Idle and wait until the queue server syncs
        synchronized(profile.stateUpdateLock) {
            profile.state = PlayerState.Idle
            profile.maintainStateTimeout = System.currentTimeMillis() + 1000L
        }
    }

    fun joinQueue(kit: Kit, queueType: QueueType, teamSize: Int, player: Player)
    {
        val profile = PracticeProfileService.find(player)
            ?: return

        val lobbyPlayer = LobbyPlayerService
            .find(player)
            ?: return@scope

        createMessage(
            packet = "join",
            "entry" to QueueEntry(
                leader = player.uniqueId,
                leaderPing = MinecraftReflection.getPing(player),
                leaderELO = profile.getRankedStatsFor(kit).elo,
                defaultPingDiff = player.pingRange.sanitizedDiffsBy(),
                defaultELODiff = player.eloRange.sanitizedDiffsBy(),
                players = listOf(player.uniqueId)
            ),
            "kit" to kit.id,
            "queueType" to queueType,
            "teamSize" to teamSize
        ).publish(
            context = AwareThreadContext.SYNC
        )

        // set InQueue and wait until the queue server syncs
        synchronized(profile.stateUpdateLock) {
            profile.state = PlayerState.InQueue
            profile.maintainStateTimeout = System.currentTimeMillis() + 1000L
        }
    }
}
