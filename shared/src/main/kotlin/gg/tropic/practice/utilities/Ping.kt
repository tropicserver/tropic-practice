package gg.tropic.practice.utilities

/**
 * @author GrowlyX
 * @since 12/24/2023
 */
fun formatPlayerPing(ping: Int) = when (true)
{
    (ping > 110) -> "&c"
    (ping > 60) -> "&e"
    else -> "&a"
}
