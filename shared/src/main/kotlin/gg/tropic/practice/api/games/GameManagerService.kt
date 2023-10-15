package gg.tropic.practice.api.games

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.models.GameStatus
import gg.tropic.practice.games.models.GameStatusIndexes
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
@Service
object GameManagerService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:gamemanager")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, aware, *pairs)

    fun bindToStatusService(statusService: () -> GameStatus)
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
            }, 0L, 5L)
            .bindWith(plugin)

        plugin.logger.info("Bound status service. Status updates for available games will be pushed to the gamemanager channel ever 0.25 seconds.")
    }

    fun allGames() = allGameStatuses()
        .thenApply {
            it.values
                .flatMap(GameStatus::games)
        }

    fun allGameStatuses() = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .get("tropicpractice:gamemanager:status-indexes")
                .let {
                    Serializers.gson.fromJson(
                        it, GameStatusIndexes::class.java
                    )
                }
                .indexes
                .mapValues { (_, value) ->
                    ScalaCommonsSpigot.instance.kvConnection
                        .sync()
                        .get(value)
                        .let {
                            Serializers.gson.fromJson(
                                it, GameStatus::class.java
                            )
                        }
                }
        }

    @Configure
    fun configure()
    {
        aware.connect().toCompletableFuture().join()
    }
}
