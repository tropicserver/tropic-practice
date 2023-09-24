package gg.tropic.practice.replications

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.serializers.Serializers
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@Service
object ReplicationManagerService : CompositeTerminable by CompositeTerminable.create()
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:replicationmanager")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    fun bindToStatusService(statusService: () -> ReplicationStatus)
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                createMessage(
                    packet = "status",
                    "server" to ServerSync.local.id,
                    "status" to Serializers.gson
                        .toJson(statusService())
                ).publish(
                    context = AwareThreadContext.SYNC
                )
            }, 0L, 10L)
            .bindWith(this)

        plugin.logger.info("Bound status service. Status updates for available replications will be pushed to the replicationmanager channel ever 0.5 seconds.")
    }

    @Configure
    fun configure()
    {
        // TODO: do we need to do this? every server will be spammed lol
        aware
//        aware.connect().toCompletableFuture().join()
    }
}
