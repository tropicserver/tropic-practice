package gg.tropic.practice.kit

import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
class Kit(
    val id: String,
    val displayName: String,
    val displayIcon: ItemStack = ItemBuilder
        .of(Material.POTATO)
        .build(),
    var enabled: Boolean = false,
    var armorContents: Array<ItemStack?> = arrayOfNulls(4),
    var contents: Array<ItemStack?> = arrayOfNulls(36),
    var additionalContents: List<ItemStack> = listOf(),
    val features: MutableMap<FeatureFlag, MutableMap<String, String>> = mutableMapOf()
)
