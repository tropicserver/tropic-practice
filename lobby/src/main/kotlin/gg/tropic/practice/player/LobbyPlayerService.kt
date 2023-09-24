package gg.tropic.practice.player

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import me.lucko.helper.Events
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

@Service
object LobbyPlayerService
{
    @Inject
    lateinit var plugin: PracticeLobby
    private val playerCache = mutableMapOf<UUID, LobbyPlayer>()

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
    }

    fun find(uniqueId: UUID) = playerCache[uniqueId]
}