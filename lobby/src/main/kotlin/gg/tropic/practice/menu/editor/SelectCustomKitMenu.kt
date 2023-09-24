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
import org.bukkit.event.inventory.ClickType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
                .name("${CC.GREEN}${loadoutAt.name}")
                .addToLore(
                    "${CC.GRAY}Last edited: ${CC.WHITE}${
                        DATE_FORMAT.format(dateCreated)
                    }",
                    "",
                    "${CC.B_RED}Shift-click to delete!",
                    "${CC.GREEN}Click to edit!"
                )
                .toButton { _, type ->
                    if (type!!.isShiftClick)
                    {
                        practiceProfile.customLoadouts[kit.id]?.remove(loadoutAt)

                        practiceProfile.save().thenRun {
                            player.sendMessage(
                                "${CC.GREEN}You have just deleted the ${CC.YELLOW}${loadoutAt.name} ${CC.GREEN}loadout for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
                            )

                            val newLoadouts = practiceProfile.getLoadoutsFromKit(kit)

                            if (newLoadouts.size == 0)
                            {
                                EditorKitSelectionMenu(practiceProfile).openMenu(player)
                            } else
                            {
                                SelectCustomKitMenu(
                                    practiceProfile,
                                    newLoadouts,
                                    kit
                                ).openMenu(player)
                            }
                        }
                        return@toButton
                    }

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
