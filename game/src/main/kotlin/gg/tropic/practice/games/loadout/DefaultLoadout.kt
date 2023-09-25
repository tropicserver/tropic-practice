package gg.tropic.practice.games.loadout

import gg.tropic.practice.kit.Kit
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
data class DefaultLoadout(private val kit: Kit) : SelectedLoadout
{
    override fun displayName() = "${CC.D_GREEN}Default"
    override fun apply(player: Player)
    {
        kit.populateAndUpdate(player)
    }
}
