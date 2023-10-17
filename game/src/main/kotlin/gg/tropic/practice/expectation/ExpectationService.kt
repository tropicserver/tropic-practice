package gg.tropic.practice.expectation

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameService
import gg.tropic.practice.resetAttributes
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
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
                // TODO: spectator pipeline
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
            .subscribe(PlayerSpawnLocationEvent::class.java)
            .handler {
                val game = GameService.byPlayer(it.player)
                    ?: return@handler

                with(game) {
                    val location = map
                        .findSpawnLocationMatchingTeam(
                            getTeamOf(it.player).side
                        )!!
                        .toLocation(arenaWorld)

                    it.spawnLocation = location
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerTeleportEvent::class.java)
            .filter {
                it.cause == PlayerTeleportEvent.TeleportCause.PLUGIN
            }
            .handler {
                val game = GameService.byPlayer(it.player)
                    ?: return@handler

                if (it.to.world.name != game.arenaWorldName)
                {
                    it.isCancelled = true
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.resetAttributes()
                it.player.removeMetadata("spectator", plugin)

                val game = GameService.byPlayer(it.player)
                    ?: return@handler

                println("PRELIM: When the game ID is ${
                    game.arenaWorldName
                }, the waiting lobby ID is ${
                    it.player.location.world.name
                }. (${
                    it.player.world.name == game.arenaWorldName
                }) (player location now ${
                    it.player.name
                })")

                with(game) {
                    val location = map
                        .findSpawnLocationMatchingTeam(
                            getTeamOf(it.player).side
                        )!!
                        .toLocation(arenaWorld)

                    Tasks.sync {
                        println("BEFORE: When the game ID is ${
                            game.arenaWorldName
                        }, the map ID is ${
                            it.player.location.world.name
                        }. (${
                            it.player.location.world.name == game.arenaWorldName
                        }) (player location now ${
                            it.player.name
                        })")

                        it.player.teleport(location)

                        println("AFTER: When the game ID is ${
                            game.arenaWorldName
                        }, the map ID is ${
                            it.player.location.world.name
                        }. (${
                            it.player.location.world.name == game.arenaWorldName
                        }) (player location now ${
                            it.player.name
                        })")
                    }
                }
            }
            .bindWith(plugin)
    }
}
