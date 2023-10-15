package gg.tropic.practice.category.restriction

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/15/2023
 */
enum class RangeRestriction(private val diffsBy: Int) : SettingValue
{
    None(-1),
    _10(10),
    _50(50),
    _100(100),
    _150(150),
    _200(200);

    override val displayName: String
        get() = if (this == None) name else diffsBy.toString()

    override fun display(player: Player) = true

    fun sanitizedDiffsBy() =
        // assume no one is going to have 2 million ping/ELO
        if (this == None) 2_000_000 else diffsBy
}
