package gg.tropic.practice.friendship

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
enum class FriendshipStateSetting(newDisplayName: String? = null) : SettingValue
{
    Enabled,
    FriendsOnly("Friends Only"),
    Disabled;

    override val displayName = newDisplayName ?: name
    override fun display(player: Player) = true
}
