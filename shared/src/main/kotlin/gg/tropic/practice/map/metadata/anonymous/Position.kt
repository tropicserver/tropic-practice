package gg.tropic.practice.map.metadata.anonymous

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
data class Position(
    val x: Double, val y: Double, val z: Double,
    val yaw: Float, val pitch: Float
)
{
    fun toVector() = Vector(x, y, z)
    fun toLocation(world: World) = Location(world, x, y, z, yaw, pitch)
}
