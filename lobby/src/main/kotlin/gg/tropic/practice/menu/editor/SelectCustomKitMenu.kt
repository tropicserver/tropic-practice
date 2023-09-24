package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SelectCustomKitMenu(
    private val practiceProfile: PracticeProfile,
    private val currentLoadouts: ConcurrentHashMap<String, Loadout>,
    private val kit: Kit
) : Menu()
{
    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for (int in 0 until 27)
        {
            buttons[int] = PaginatedMenu.PLACEHOLDER
        }

        for (int in 10 until 17)
        {
            val loadoutAt = currentLoadouts.entries.elementAt(int)
            val dateCreated = Date(loadoutAt.value.timestamp)

            //TODO: use a non-deprecated method for this
            buttons[int] = ItemBuilder
                .of(Material.PAPER)
                .addToLore(
                    "${CC.GRAY}Last edited: ${CC.WHITE}${dateCreated.month}/${dateCreated.day}/${dateCreated.year}",
                    "",
                    "${CC.B_RED}Shift-click to delete!",
                    "${CC.GREEN}Click to edit!"
                )
                .toButton { _, _ ->
                    EditLoadoutContentsMenu(kit, loadoutAt.value, practiceProfile).openMenu(player)
                }
        }

        return buttons
    }

    override fun getTitle(player: Player): String = "Choose a loadout to edit..."

    override fun size(buttons: Map<Int, Button>): Int = 27
}