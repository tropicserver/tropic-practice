package gg.minequest.duels.shared.game

import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/9/2022
 */
class GameReportSnapshot(player: Player)
{
    val inventoryContents = player.inventory.contents
        .map { it to (it?.amount ?: 1) }

    val armorContents = player.inventory.armorContents

    val gameMode = player.gameMode
    val health = player.health
    val foodLevel = player.foodLevel

    val flying = player.isFlying
    val allowFly = player.allowFlight
}
