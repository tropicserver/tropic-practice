package gg.minequest.duels.external.provider.impl

import gg.scala.lemon.handler.PlayerHandler
import gg.minequest.duels.external.provider.SettingProvider
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
object LemonSettingProvider : SettingProvider
{
    override fun provideSetting(player: Player, setting: String): Boolean
    {
        return PlayerHandler.find(player.uniqueId)?.hasMetadata(setting) ?: false
    }
}
