package gg.tropic.practice.settings.restriction

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/15/2023
 */
enum class RangeRestriction(private val diffsBy: Int) : SettingValue
{
    _10(10),
    _50(50),
    _100(100),
    _150(150),
    _200(200),
    None(-1);

    override val displayName: String
        get() = if (this == None) name else diffsBy.toString()

    override fun display(player: Player) = true

    fun sanitizedDiffsBy() = diffsBy
}
