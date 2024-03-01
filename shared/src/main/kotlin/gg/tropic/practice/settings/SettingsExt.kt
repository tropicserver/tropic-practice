package gg.tropic.practice.settings

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.tropic.practice.settings.scoreboard.ScoreboardStyle
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 1/16/2024
 */
fun Player.isASilentSpectator(): Boolean
{
    val basicsProfile = BasicsProfileService.find(this)
        ?: return false

    return basicsProfile
        .setting<StateSettingValue>("${DuelsSettingCategory.DUEL_SETTING_PREFIX}:silent-spectator") == StateSettingValue.ENABLED
        && hasPermission("practice.silent-spectator")
}

fun layout(player: Player): ScoreboardStyle
{
    return BasicsProfileService
        .find(player)
        ?.setting(
            "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:scoreboard-style",
            ScoreboardStyle.Default
        )
        ?: ScoreboardStyle.Default
}
