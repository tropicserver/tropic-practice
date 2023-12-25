package gg.tropic.practice.services

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 12/25/2023
 */
@Service
object TournamentManagerService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:tournaments")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    @Configure
    fun configure()
    {
        aware.connect()
    }

    fun publish(
        id: String,
        vararg data: Pair<String, Any>
    ) = CompletableFuture
        .runAsync {
            AwareMessage
                .of(
                    packet = id,
                    aware,
                    *data
                )
                .publish(
                    AwareThreadContext.SYNC
                )
        }
}
