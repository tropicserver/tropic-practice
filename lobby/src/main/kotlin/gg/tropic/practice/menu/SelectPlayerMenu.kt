package gg.tropic.practice.menu

import gg.tropic.practice.commands.DuelGamesCommand
import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 9/16/2022
 */
class SelectPlayerMenu(
    private val game: GameReport
) : PaginatedMenu()
{
    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.delayed(1L) {
                DuelGamesCommand.onDefault(player)
            }
        }
    }

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        game.snapshots
            .onEach {
                buttons[buttons.size] = ItemBuilder
                    .of(Material.SKULL_ITEM)
                    .data(3)
                    .owner(it.key.username())
                    .name("${CC.B_GOLD}${it.key.username()}")
                    .addToLore(
                        "${CC.WHITE}This player is on the:",
                        "${CC.GRAY}${if (it.key in game.losers) "losing" else "winning"} team",
                        "",
                        "${CC.GREEN}Click to view inventory!"
                    )
                    .toButton { _, _ ->
                        PlayerViewMenu(game, it.value).openMenu(player)
                    }
            }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) = "Previous Duels ${Constants.DOUBLE_ARROW_RIGHT} Select a Player"
}
