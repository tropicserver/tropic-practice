package gg.tropic.practice.games

import gg.tropic.practice.games.counter.Counter
import gg.tropic.practice.kit.Kit
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/9/2022
 */
class GameReportSnapshot(player: Player, counter: Counter, kit: Kit)
{
    val inventoryContents = player.inventory.contents
        .map { it to (it?.amount ?: 1) }

    val armorContents = player.inventory.armorContents
    val potionEffects = player.activePotionEffects

    val healthPotions = player.inventory.contents
        .filterNotNull()
        .count {
            it.type == Material.POTION && it.durability.toInt() == 16421
        }

    val containsHealthPotions = kit.contents
        .filterNotNull()
        .any {
            it.type == Material.POTION && it.durability.toInt() == 16421
        }

    val missedPotions = counter.valueOf("missedPots").toInt()
    val wastedHeals = counter.valueOf("wastedHeals")

    val hitPotions = counter.valueOf("hitPots").toInt()
    val totalPotionsUsed = counter.valueOf("totalPots").toInt()

    val mushroomStews = player.inventory.contents
        .filterNotNull()
        .count {
            it.type == Material.MUSHROOM_SOUP
        }

    val containsMushroomStews = kit.contents
        .filterNotNull()
        .any {
            it.type == Material.MUSHROOM_SOUP
        }

    val health = player.health
    val foodLevel = player.foodLevel
}
