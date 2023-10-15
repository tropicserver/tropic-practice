package gg.tropic.practice.menu

import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 9/16/2022
 */
class SelectPlayerMenu(
    private val game: GameReport,
    private val gamesMenu: Menu
) : PaginatedMenu()
{
    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.sync {
                gamesMenu.openMenu(player)
            }
        }
    }

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        game.snapshots
            .entries
            .sortedByDescending { it.key in game.losers }
            .onEach {
                buttons[buttons.size] = ItemBuilder
                    .of(Material.SKULL_ITEM)
                    .data(3)
                    .owner(it.key.username())
                    .name("${CC.B_GREEN}${it.key.username()}")
                    .addToLore(
                        "${CC.WHITE}This player is on the:",
                        "${CC.GREEN}${if (it.key in game.losers) "losing" else "winning"} team",
                        "",
                        "${CC.GREEN}Click to view inventory!"
                    )
                    .toButton { _, _ ->
                        PlayerViewMenu(game, it.value, gamesMenu).openMenu(player)
                    }
            }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) = "Select a player..."
}
