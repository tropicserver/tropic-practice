package gg.tropic.practice.menu.template

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
abstract class TemplateKitMenu : PaginatedMenu()
{
    init
    {
        placeholder = true
    }

    abstract fun filterDisplayOfKit(player: Player, kit: Kit): Boolean
    abstract fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    abstract fun itemClicked(player: Player, kit: Kit, type: ClickType)

    open fun itemTitleExtensionOf(player: Player, kit: Kit): String
    {
        return ""
    }

    override fun size(buttons: Map<Int, Button>) = 54
    override fun getMaxItemsPerPage(player: Player) = 21

    override fun getAllPagesButtonSlots() =
        (11..17) + (20..26) + (29..35)

    override fun getPageButtonSlots() = 19 to 27

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        KitService
            .cached()
            .kits
            .values
            .filter {
                it.enabled && filterDisplayOfKit(player, it)
            }
            .sortedByDescending {
                it.featureConfig(FeatureFlag.MenuOrderWeight, "weight")
                    .toInt()
            }
            .forEach {
                buttons[buttons.size] = ItemBuilder
                    .copyOf(it.displayIcon)
                    .name("${CC.GREEN}${it.displayName}${
                        if (it.features(FeatureFlag.NewlyCreated)) 
                            " ${CC.B_YELLOW}NEW!" else ""
                    }${
                        itemTitleExtensionOf(player, it)
                    }")
                    .setLore(itemDescriptionOf(player, it))
                    .toButton { _, type ->
                        itemClicked(player, it, type!!)
                    }
            }

        return buttons
    }
}
