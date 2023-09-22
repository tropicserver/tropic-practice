package gg.tropic.practice.kit.group

import gg.tropic.practice.kit.KitService

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
data class KitGroup(
    val id: String,
    val contains: MutableList<String> = mutableListOf()
)
{
    fun kits() = contains
        .mapNotNull {
            KitService.cached().kits[it]
        }
}
