package gg.tropic.practice.expectation

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameService
import gg.tropic.practice.resetAttributes
import gg.tropic.spa.SPABindings
import me.lucko.helper.Events
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.visibility.VisibilityHandler
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInitialSpawnEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.metadata.FixedMetadataValue
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
        SPABindings.filterPlayerMoveIntoWorld { player, location ->
            val game = GameService
                .byPlayer(player.uniqueId)
                ?: return@filterPlayerMoveIntoWorld location.world.name == "world"

            location.world.name == "world" ||
                location.world.name == game.arenaWorldName
        }

        Events
            .subscribe(
                AsyncPlayerPreLoginEvent::class.java,
                EventPriority.MONITOR
            )
            .handler { event ->
                val game = GameService
                    .byPlayerOrSpectator(event.uniqueId)

                if (game == null)
                {
                    event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        "${CC.RED}You do not have a game to join!"
                    )
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerInitialSpawnEvent::class.java)
            .handler {
                with(GameService.byPlayer(it.player)) {
                    if (this != null)
                    {
                        it.spawnLocation = map
                            .findSpawnLocationMatchingTeam(
                                getTeamOf(it.player).side
                            )!!
                            .toLocation(arenaWorld)
                    } else
                    {
                        val spectatorGame = GameService
                            .bySpectator(it.player.uniqueId)

                        if (spectatorGame != null)
                        {
                            it.spawnLocation = spectatorGame
                                .toBukkitPlayers()
                                .filterNotNull()
                                .first().location
                        }
                    }
                }

            }
            .bindWith(plugin)


        Events
            .subscribe(
                PlayerJoinEvent::class.java,
                EventPriority.MONITOR
            )
            .handler {
                val game = GameService
                    .byPlayerOrSpectator(it.player.uniqueId)
                    ?: return@handler

                it.player.resetAttributes()

                if (it.player.uniqueId in game.expectedSpectators)
                {
                    it.player.setMetadata(
                        "spectator",
                        FixedMetadataValue(plugin, true)
                    )

                    NametagHandler.reloadPlayer(it.player)
                    VisibilityHandler.update(it.player)

                    it.player.allowFlight = true
                    it.player.isFlying = true

                    game.sendMessage(
                        "${CC.GREEN}${it.player.name}${CC.YELLOW} is now spectating the game."
                    )

                    it.player.sendMessage(
                        "${CC.B_YELLOW}You are now spectating the game."
                    )
                } else
                {
                    it.player.removeMetadata("spectator", plugin)
                }
            }
            .bindWith(plugin)
    }
}
