package gg.tropic.practice.queue

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
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

    fun joinQueue(kit: Kit, player: Player)
    {
        createMessage(
            packet = "join",
            "entry" to QueueEntry(
                leader = player.uniqueId,
                leaderPing = MinecraftReflection.getPing(player),
                players = listOf(player.uniqueId)
            ),
            "kit" to kit.id,
            "queueType" to QueueType.Casual,
            "teamSize" to 1
        ).publish(
            context = AwareThreadContext.SYNC
        )
    }
}
