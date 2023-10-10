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
import me.lucko.helper.Schedulers
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.Color
import org.bukkit.Bukkit
import org.bukkit.entity.Player
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
            .of<AwareMessage>("practice:lobbies")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                Players.all()
                    .mapNotNull(::find)
                    .forEach(LobbyPlayer::syncQueueState)
            }, 0L, 5L)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                playerCache[event.player.uniqueId] =
                    LobbyPlayer(event.player.uniqueId)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                playerCache.remove(event.player.uniqueId)
            }
            .bindWith(plugin)

        fun AwareMessage.usePlayer(use: Player.() -> Unit)
        {
            val players = retrieve<List<String>>("playerIDs")
            players.forEach {
                val bukkit = Bukkit
                    .getPlayer(
                        UUID.fromString(it)
                    )
                    ?: return

                use(bukkit)
            }
        }

        aware.listen("send-message") {
            usePlayer {
                val message = retrieve<List<String>>("message")
                message.forEach {
                    sendMessage(
                        Color.translate(
                            it
                                .replace("{primary}", CC.PRI)
                                .replace("{secondary}", CC.SEC)
                        )
                    )
                }
            }
        }

        aware.listen("redirect") {
            usePlayer {
                VelocityRedirectSystem.redirect(
                    this, retrieve("server")
                )
            }
        }
        aware.connect().toCompletableFuture().join()
    }

    fun find(uniqueId: UUID) = playerCache[uniqueId]
    fun find(player: Player) = playerCache[player.uniqueId]
}
