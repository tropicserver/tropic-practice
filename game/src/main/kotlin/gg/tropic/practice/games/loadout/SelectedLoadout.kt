package gg.tropic.practice.games.loadout

import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
interface SelectedLoadout
{
    fun displayName(): String
    fun apply(player: Player)
}
