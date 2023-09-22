package gg.tropic.practice.kit

import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
data class Kit(
    val id: String, val displayName: String, var enabled: Boolean = false,
    val icon: ItemStack = ItemBuilder
        .of(Material.IRON_SWORD)
        .build(),
    var contents: List<ItemStack> = listOf(),
    var additionalContents: List<ItemStack> = listOf(),
    val features: MutableMap<FeatureFlag, MutableMap<String, String>> = mutableMapOf()
)
