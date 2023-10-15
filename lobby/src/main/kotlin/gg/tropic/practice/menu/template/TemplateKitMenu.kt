package gg.tropic.practice.menu.template

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.text.TextSplitter
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
abstract class TemplateKitMenu : PaginatedMenu()
{
    init
    {
        placeholdBorders = true
    }

    abstract fun filterDisplayOfKit(player: Player, kit: Kit): Boolean
    abstract fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    abstract fun itemClicked(player: Player, kit: Kit, type: ClickType)

    open fun itemTitleExtensionOf(player: Player, kit: Kit): String
    {
        return ""
    }

    override fun size(buttons: Map<Int, Button>) = 45
    override fun getMaxItemsPerPage(player: Player) = 21

    override fun getAllPagesButtonSlots() =
        (10..16) + (19..25) + (28..34)

    override fun getPageButtonSlots() = 18 to 27

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
                    .name(
                        "${CC.B_GREEN}${it.displayName}${
                            if (it.features(FeatureFlag.NewlyCreated))
                                " ${CC.B_YELLOW}NEW!" else ""
                        }${
                            itemTitleExtensionOf(player, it)
                        }"
                    )
                    .apply {
                        if (it.description.isNotBlank())
                        {
                            setLore(
                                TextSplitter.split(
                                    it.description,
                                    CC.GRAY, " "
                                )
                            )
                            addToLore("")
                        }
                    }
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
