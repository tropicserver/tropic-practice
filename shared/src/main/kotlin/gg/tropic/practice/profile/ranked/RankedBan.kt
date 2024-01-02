package gg.tropic.practice.profile.ranked

import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 1/1/2024
 */
data class RankedBan(var effectiveUntil: Long? = null)
{
    fun isEffective() = effectiveUntil == null ||
        System.currentTimeMillis() < effectiveUntil!!

    fun deliverBanMessage(player: Player)
    {
        player.sendMessage("${CC.RED}You currently have a ranked ban.")

        if (effectiveUntil != null)
        {
            player.sendMessage(
                CC.RED + "Effective for: " + CC.WHITE + TimeUtil.formatIntoAbbreviatedString(
                    (effectiveUntil!! - System.currentTimeMillis()).toInt() / 1000
                )
            )
        } else
        {
            player.sendMessage(CC.RED + "This ranked-ban is effective forever.")
        }
    }
}
