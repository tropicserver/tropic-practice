package gg.tropic.practice.reports.menu

import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.games.GameReport
import gg.tropic.practice.games.GameReportSnapshot
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/16/2022
 */
class PlayerViewMenu(
    private val gameReport: GameReport,
    private val reportOf: UUID,
    private val snapshot: GameReportSnapshot,
    private val originalMenu: Menu? = null
) : Menu("Viewing player ${reportOf.username()}...")
{
    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.delayed(1L) {
                SelectPlayerMenu(gameReport, originalMenu).openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for (i in 45 until 54)
        {
            buttons[i] = PaginatedMenu.PLACEHOLDER
        }

        buttons[47] = ItemBuilder.of(Material.GOLDEN_APPLE)
            .name("${CC.SEC}Health: ${CC.GREEN}${"%.1f".format(snapshot.health)} ${Constants.HEART_SYMBOL}")
            .toButton()

        buttons[47] = ItemBuilder.of(Material.GOLDEN_CARROT)
            .name("${CC.WHITE}Food Level: ${CC.RED}${snapshot.foodLevel}")
            .toButton()

        snapshot.inventoryContents.withIndex()
            .forEach {
                if (it.value.first != null)
                {
                    buttons[it.index] = ItemBuilder
                        .copyOf(it.value.first)
                        .amount(it.value.second)
                        .toButton()
                }
            }

        for (i in 49..52)
        {
            val armor = snapshot.armorContents
                .getOrNull(i - 49)

            if (armor == null)
            {
                buttons[i] = ItemBuilder
                    .of(Material.BARRIER)
                    .name("${CC.BD_RED}No item")
                    .addToLore(
                        "${CC.WHITE}This player did not have",
                        "${CC.WHITE}an item in this armor slot!"
                    )
                    .toButton()
                continue
            }

            buttons[i] = ItemBuilder
                .copyOf(armor)
                .glow()
                .toButton()
        }

        return buttons
    }

    override fun size(buttons: Map<Int, Button>) = 54
}
