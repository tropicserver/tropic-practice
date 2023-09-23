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
data class Kit(
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

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kit

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (displayIcon != other.displayIcon) return false
        if (enabled != other.enabled) return false
        if (!armorContents.contentEquals(other.armorContents)) return false
        if (!contents.contentEquals(other.contents)) return false
        if (!additionalContents.contentEquals(other.additionalContents)) return false
        if (features != other.features) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + displayIcon.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + armorContents.contentHashCode()
        result = 31 * result + contents.contentHashCode()
        result = 31 * result + additionalContents.contentHashCode()
        result = 31 * result + features.hashCode()
        return result
    }
}
