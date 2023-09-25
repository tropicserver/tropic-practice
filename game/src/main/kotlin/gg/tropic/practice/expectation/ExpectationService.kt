package gg.tropic.practice.expectation

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameService
import gg.tropic.practice.resetAttributes
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Service
object ExpectationService
{
    @Inject
    lateinit var plugin: PracticeGame

    @Inject
    lateinit var audiences: BukkitAudiences

    @Configure
    fun configure()
    {
        Events
            .subscribe(
                AsyncPlayerPreLoginEvent::class.java,
                EventPriority.HIGHEST
            )
            .handler { event ->
                GameService.byPlayer(event.uniqueId)
                    ?: return@handler run {
                        event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                            "${CC.RED}You have no game scheduled for you!"
                        )
                    }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.resetAttributes()
                it.player.removeMetadata("spectator", plugin)
            }

        Events
            .subscribe(
                PlayerSpawnLocationEvent::class.java,
                EventPriority.HIGHEST
            )
            .handler {
                val game = GameService.byPlayer(it.player)
                    ?: return@handler

                it.spawnLocation = with(game) {
                    map
                        .findSpawnLocationMatchingTeam(
                            getTeamOf(it.player).side
                        )!!
                        .toLocation(arenaWorld)
                }
            }
            .bindWith(plugin)
    }
}
