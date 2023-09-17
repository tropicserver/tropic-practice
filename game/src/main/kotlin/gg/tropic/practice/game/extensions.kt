package gg.tropic.practice.game

import org.bukkit.GameMode
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
fun Player.resetAttributes()
{
    this.health = this.maxHealth

    this.foodLevel = 20
    this.saturation = 12.8f
    this.maximumNoDamageTicks = 20
    this.fireTicks = 0
    this.fallDistance = 0.0f
    this.level = 0
    this.exp = 0.0f
    this.walkSpeed = 0.2f
    this.inventory.heldItemSlot = 0
    this.allowFlight = false

    this.inventory.clear()
    this.inventory.armorContents = null

    this.closeInventory()

    this.gameMode = GameMode.SURVIVAL
    this.fireTicks = 0

    for (potionEffect in this.activePotionEffects)
    {
        this.removePotionEffect(potionEffect.type)
    }

    this.updateInventory()
}
