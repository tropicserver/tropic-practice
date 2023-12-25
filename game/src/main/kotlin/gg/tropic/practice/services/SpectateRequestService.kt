package gg.tropic.practice.services

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.spectate.SpectateRequest
import java.util.*
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 10/20/2023
 */
@Service
object SpectateRequestService
{
    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:queue-inhabitants")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, aware, *pairs)

    @Configure
    fun configure()
    {
        aware.listen("request-spectate") {
            val server = retrieve<String>("server")
            if (ServerSync.local.id != server)
            {
                return@listen
            }

            val request = retrieve<SpectateRequest>("request")
            val game = retrieve<UUID>("game")
            val requestID = retrieve<UUID>("requestID")

            val gameImpl = GameService.games[game]
                ?: return@listen

            gameImpl.expectedSpectators += request.player

            createMessage(
                "spectate-ready",
                "requestID" to requestID
            ).publish(
                AwareThreadContext.SYNC,
                "practice:queue"
            )
        }
        aware.connect().toCompletableFuture().join()
    }
}
