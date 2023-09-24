package gg.tropic.practice.map

import org.bukkit.World

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
data class BuiltMapReplication(
    val associatedMap: Map,
    val world: World,
    var inUse: Boolean = false
)
