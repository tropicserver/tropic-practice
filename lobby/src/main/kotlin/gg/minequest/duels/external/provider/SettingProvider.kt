package gg.minequest.duels.external.provider

import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
interface SettingProvider
{
    fun provideSetting(player: Player, setting: String): Boolean
}
