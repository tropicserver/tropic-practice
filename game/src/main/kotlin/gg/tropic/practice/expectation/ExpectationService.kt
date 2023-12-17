package gg.tropic.practice.expectation

import com.cryptomorin.xseries.XMaterial
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameService
import gg.tropic.practice.resetAttributes
import me.lucko.helper.Events
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.visibility.VisibilityHandler
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.metadata.FixedMetadataValue

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
            .subscribe(PlayerInteractEvent::class.java)
            .filter {
                it.hasItem() && (
                    it.action == Action.RIGHT_CLICK_BLOCK ||
                        it.action == Action.RIGHT_CLICK_AIR
                    )
            }
            .filter {
                it.player.hasMetadata("spectator")
            }
            .handler {
                GameService.redirector.redirect(it.player)
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

                val spawnLocation = if (it.player.uniqueId !in game.expectedSpectators)
                {
                    game.map
                        .findSpawnLocationMatchingTeam(
                            game.getTeamOf(it.player).side
                        )!!
                        .toLocation(game.arenaWorld)
                } else
                {
                    game
                        .toBukkitPlayers()
                        .filterNotNull()
                        .first().location
                }

                it.player.teleport(spawnLocation)
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

                    it.player.inventory.setItem(
                        8,
                        ItemBuilder.of(XMaterial.RED_DYE)
                            .name("${CC.RED}Return to Spawn ${CC.GRAY}(Right Click)")
                            .build()
                    )
                    it.player.updateInventory()

                    game.sendMessage(
                        "${CC.GREEN}${it.player.name}${CC.SEC} is now spectating the game."
                    )

                    it.player.sendMessage(
                        "${CC.GREEN}You are now spectating the game."
                    )
                } else
                {
                    it.player.removeMetadata("spectator", plugin)
                }
            }
            .bindWith(plugin)
    }
}
