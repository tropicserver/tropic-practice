package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

class SelectCustomKitMenu(
    private val practiceProfile: PracticeProfile,
    private val currentLoadouts: MutableList<Loadout>,
    private val kit: Kit
) : Menu()
{
    companion object
    {
        @JvmStatic
        val DATE_FORMAT = SimpleDateFormat()
    }

    init
    {
        placeholder = true
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for ((index, loadoutAt) in currentLoadouts.withIndex())
        {
            val dateCreated = Date(loadoutAt.timestamp)

            buttons[index + 10] = ItemBuilder
                .of(Material.PAPER)
                .name("${CC.B_AQUA}${loadoutAt.name}")
                .addToLore(
                    "${CC.WHITE}Last edited:",
                    "${CC.AQUA}${
                        DATE_FORMAT.format(dateCreated)
                    }",
                    "",
                    "${CC.GREEN}Click to edit!"
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    EditLoadoutContentsMenu(kit, loadoutAt, practiceProfile).openMenu(player)
                }
        }

        return buttons
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.sync {
                EditorKitSelectionMenu(practiceProfile).openMenu(player)
            }
        }
    }

    override fun getTitle(player: Player): String = "Choose a loadout to edit..."
    override fun size(buttons: Map<Int, Button>): Int = 27
}
