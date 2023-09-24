package gg.tropic.practice.map

import org.bukkit.World
import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
data class BuiltMapReplication(
    val associatedMap: Map,
    val world: World,
    var scheduledForExpectation: UUID? = null,
    var inUse: Boolean = false
)
