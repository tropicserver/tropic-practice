package gg.tropic.practice.settings.scoreboard

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

/**
 * @author DripW
 * @since 2/29/2024
 */
enum class ScoreboardStyle : SettingValue {
    DEFAULT, LEGACY;

    override val displayName: String
        get() = name.lowercase().capitalize()

    override fun display(player: Player) = true
}

