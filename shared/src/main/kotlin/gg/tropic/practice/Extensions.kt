package gg.tropic.practice

import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 3/2/2024
 */
fun Array<ItemStack?>.deepClone(): Array<ItemStack?>
{
    val copy = arrayOfNulls<ItemStack?>(size = size)
    forEachIndexed { index, itemStack ->
        copy[index] = itemStack?.clone()
    }

    return copy
}
