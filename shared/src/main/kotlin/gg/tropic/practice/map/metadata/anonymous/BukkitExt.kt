package gg.tropic.practice.map.metadata.anonymous

import org.bukkit.Location
import org.bukkit.util.Vector

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
fun Location.toPosition() = Position(
    x = x,
    y = y,
    z = z,
    yaw = yaw,
    pitch = pitch
)

fun Vector.toPosition() = Position(
    x = x,
    y = y,
    z = z,
    yaw = 0f,
    pitch = 0f
)
