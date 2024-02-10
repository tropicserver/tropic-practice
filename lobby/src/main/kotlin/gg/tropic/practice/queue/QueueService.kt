package gg.tropic.practice.queue

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.category.pingRange
import gg.tropic.practice.games.spectate.SpectateRequest
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import gg.tropic.practice.region.Region
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.util.CC
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
            .of<AwareMessage>("practice:queue".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    @Configure
    fun configure()
    {
        this.aware.connect()
            .toCompletableFuture()
            .join()
    }

    fun leaveQueue(player: Player, force: Boolean = false)
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
            context = AwareThreadContext.ASYNC
        )

        if (force)
        {
            return
        }

        // set Idle and wait until the queue server syncs
        synchronized(lobbyPlayer.stateUpdateLock) {
            lobbyPlayer.state = PlayerState.Idle
            lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
        }
    }

    fun spectate(request: SpectateRequest)
    {
        createMessage(
            packet = "spectate",
            "request" to request
        ).publish(
            context = AwareThreadContext.SYNC
        )
    }

    fun joinQueue(kit: Kit, queueType: QueueType, teamSize: Int, player: Player)
    {
        val profile = PracticeProfileService.find(player)
            ?: return

        val lobbyPlayer = LobbyPlayerService
            .find(player)
            ?: return

        if (lobbyPlayer.state == PlayerState.InQueue)
        {
            return
        }

        PlayerRegionFromRedisProxy.of(player)
            .exceptionally { Region.NA }
            .thenAcceptAsync {
                createMessage(
                    packet = "join",
                    "entry" to QueueEntry(
                        leader = player.uniqueId,
                        leaderPing = MinecraftReflection.getPing(player),
                        queueRegion = it,
                        leaderELO = profile.getRankedStatsFor(kit).elo,
                        maxPingDiff = player.pingRange.sanitizedDiffsBy(),
                        players = listOf(player.uniqueId)
                    ),
                    "kit" to kit.id,
                    "queueType" to queueType,
                    "teamSize" to teamSize
                ).publish(
                    context = AwareThreadContext.SYNC
                )
            }
            .exceptionally {
                player.sendMessage("${CC.RED}We were unable to put you in the queue!")
                return@exceptionally null
            }

        // set InQueue and wait until the queue server syncs
        synchronized(lobbyPlayer.stateUpdateLock) {
            lobbyPlayer.state = PlayerState.InQueue
            lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
        }
    }
}
