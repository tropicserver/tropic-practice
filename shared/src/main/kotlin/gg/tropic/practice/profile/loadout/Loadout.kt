package gg.tropic.practice.profile.loadout

import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
data class Loadout(
    val name: String,
    val pairedKit: String,
    val timestamp: Long,
    val inventoryContents: Array<ItemStack?> = arrayOfNulls(36)
)
{
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Loadout

        if (name != other.name) return false
        if (pairedKit != other.pairedKit) return false
        if (!inventoryContents.contentEquals(other.inventoryContents)) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = name.hashCode()
        result = 31 * result + pairedKit.hashCode()
        result = 31 * result + inventoryContents.contentHashCode()
        return result
    }
}
