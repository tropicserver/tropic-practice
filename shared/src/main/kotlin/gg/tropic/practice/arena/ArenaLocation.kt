package gg.tropic.practice.arena

import org.bukkit.Location
import org.bukkit.World

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
class ArenaLocation(
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val yaw: Float,
    private val pitch: Float
)
{
    fun location(world: World): Location
    {
        return Location(
            world, x, y, z, yaw, pitch
        )
    }
}
