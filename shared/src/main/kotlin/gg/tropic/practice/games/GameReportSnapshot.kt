package gg.tropic.practice.games

import org.bukkit.Material
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
    val potionEffects = player.activePotionEffects

    val healthPotions = player.inventory.contents
        .filterNotNull()
        .count {
            it.type == Material.POTION
        }

    val mushroomStews = player.inventory.contents
        .filterNotNull()
        .count {
            it.type == Material.MUSHROOM_SOUP
        }

    val health = player.health
    val foodLevel = player.foodLevel
}
