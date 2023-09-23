package gg.tropic.practice.kit

import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
class Kit(
    val id: String,
    val displayName: String,
    val displayIcon: ItemStack = ItemBuilder
        .of(Material.IRON_SWORD)
        .build(),
    var enabled: Boolean = false,
    var armorContents: Array<ItemStack?> = arrayOfNulls(4),
    var contents: Array<ItemStack?> = arrayOfNulls(36),
    var additionalContents: Array<ItemStack?> = arrayOfNulls(27),
    val features: MutableMap<FeatureFlag, MutableMap<String, String>> = mutableMapOf()
)
{
    fun populate(player: Player)
    {
        player.inventory.clear()
        player.inventory.armorContents = armorContents
        player.inventory.contents = contents
        player.updateInventory()
    }
}
