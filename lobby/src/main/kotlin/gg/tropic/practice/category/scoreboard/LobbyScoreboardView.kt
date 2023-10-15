package gg.tropic.practice.category.scoreboard

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/14/2023
 */
enum class LobbyScoreboardView : SettingValue
{
    Dev, Staff, None;

    override val displayName: String
        get() = name

    override fun display(player: Player) = if (this == None)
    {
        true
    } else
    {
        player
            .hasPermission(
                "practice.lobby.scoreboard.views.$name"
            )
    }
}
