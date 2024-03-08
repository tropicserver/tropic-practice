package gg.tropic.practice.games.loadout

import gg.tropic.practice.utilities.deepClone
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.loadout.Loadout
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
class CustomLoadout(
    private val loadout: Loadout,
    private val kit: Kit
) : SelectedLoadout
{
    override fun displayName() = loadout.name
    override fun apply(player: Player)
    {
        kit.populate(player)
        player.inventory.contents = loadout.inventoryContents.deepClone()
        player.updateInventory()
    }
}
