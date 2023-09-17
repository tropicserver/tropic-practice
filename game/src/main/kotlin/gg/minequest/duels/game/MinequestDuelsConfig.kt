package gg.minequest.duels.game

import gg.scala.commons.config.annotations.Config
import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 9/11/2022
 */
@Config("config")
class MinequestDuelsConfig
{
    val waitingLocation = Location(
        Bukkit.getWorld("world"),
        243.500, 52.000, 1348.500
    )
}
