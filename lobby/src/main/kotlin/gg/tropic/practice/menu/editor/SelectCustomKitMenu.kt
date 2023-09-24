package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
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
            val loadoutAt = currentLoadouts.entries.elementAtOrNull(int - 10) ?: continue
            val dateCreated = Date(loadoutAt.value.timestamp)

            //TODO: use a non-deprecated method for this
            buttons[int] = ItemBuilder
                .of(Material.PAPER)
                .name("${CC.WHITE}${loadoutAt.key}")
                .addToLore(
                    "${CC.GRAY}Last edited: ${CC.WHITE}${dateCreated.month}/${dateCreated.day}/${dateCreated.year - 100}",
                    "",
                    "${CC.B_RED}Shift-click to delete!",
                    "${CC.GREEN}Click to edit!"
                )
                .toButton { _, type ->

                    if (type == ClickType.LEFT)
                    {
                        EditLoadoutContentsMenu(kit, loadoutAt.value, practiceProfile).openMenu(player)
                    } else if (type == ClickType.SHIFT_LEFT)
                    {
                        practiceProfile.customLoadouts[kit.id]!!.remove(loadoutAt.key)

                        practiceProfile.save().thenRun {
                            player.sendMessage(
                                "${CC.GREEN}You have just deleted the ${CC.YELLOW}${loadoutAt.key} ${CC.GREEN}loadout for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
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
                    }
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