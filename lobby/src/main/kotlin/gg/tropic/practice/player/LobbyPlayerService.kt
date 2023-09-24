package gg.tropic.practice.player

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import gg.tropic.practice.PracticeLobby
import me.lucko.helper.Events
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.logging.Logger

@Service
object LobbyPlayerService
{
    @Inject
    lateinit var plugin: PracticeLobby
    private val playerCache = mutableMapOf<UUID, LobbyPlayer>()

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:redirector")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    @Configure
    fun configure()
    {
        Events.subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                playerCache[event.player.uniqueId] = LobbyPlayer(event.player.uniqueId)
            }.bindWith(plugin)

        Events.subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                playerCache.remove(event.player.uniqueId)
            }.bindWith(plugin)

        aware.listen("redirect") {
            val player = retrieve<UUID>("playerID")
            val bukkit = Bukkit.getPlayer(player)
                ?: return@listen

            VelocityRedirectSystem.redirect(
                bukkit, retrieve("server")
            )
        }
        aware.connect().toCompletableFuture().join()
    }

    fun find(uniqueId: UUID) = playerCache[uniqueId]
}
