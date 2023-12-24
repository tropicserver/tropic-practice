package gg.tropic.practice.utilities

/**
 * @author GrowlyX
 * @since 12/24/2023
 */
object PingFormatter
{
    fun format(ping: Int) = when (true)
    {
        (ping > 110) -> "&c"
        (ping > 60) -> "&e"
        else -> "&a"
    }
}
