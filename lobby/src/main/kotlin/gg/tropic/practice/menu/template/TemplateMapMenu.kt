package gg.tropic.practice.menu.template

import gg.tropic.practice.map.MapService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag

/**
 * @author GrowlyX
 * @since 10/21/2023
 */
abstract class TemplateMapMenu : PaginatedMenu()
{
    init
    {
        placeholdBorders = true
    }

    abstract fun filterDisplayOfMap(map: gg.tropic.practice.map.Map): Boolean
    abstract fun itemDescriptionOf(player: Player, map: gg.tropic.practice.map.Map): List<String>
    abstract fun itemClicked(player: Player, map: gg.tropic.practice.map.Map, type: ClickType)

    open fun itemTitleFor(player: Player, map: gg.tropic.practice.map.Map): String
    {
        return "${CC.GREEN}${map.displayName}"
    }

    override fun size(buttons: Map<Int, Button>) = 45
    override fun getMaxItemsPerPage(player: Player) = 21

    override fun getAllPagesButtonSlots() =
        (10..16) + (19..25) + (28..34)

    override fun getPageButtonSlots() = 18 to 27

    private val filteredMaps = MapService
        .cached()
        .maps
        .values
        .filter {
            !it.locked && filterDisplayOfMap(it)
        }

    fun getAvailableMaps() = filteredMaps
    fun ensureMapsAvailable() = filteredMaps.isNotEmpty()

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        filteredMaps.forEach {
            buttons[buttons.size] = ItemBuilder
                .copyOf(it.displayIcon)
                .name(itemTitleFor(player, it))
                .addFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_POTION_EFFECTS
                )
                .addToLore(
                    *itemDescriptionOf(player, it)
                        .toTypedArray()
                )
                .toButton { _, type ->
                    itemClicked(player, it, type!!)
                }
        }

        return buttons
    }
}
