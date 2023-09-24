package gg.tropic.practice.application.api

import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
class DPSRedisService(
    private val channel: String
)
{
    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>(channel)
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    fun configure(
        lambda: Aware<AwareMessage>.() -> Unit
    )
    {
        this.aware.lambda()
    }

    fun <T> configurePopulator(
        lambda: Aware<AwareMessage>.() -> T
    ): T
    {
        return this.aware.lambda()
    }

    fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    fun start()
    {
        this.aware.connect()
            .toCompletableFuture()
            .join()
    }
}
