package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.player.hotbar.LobbyHotbarService
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class EditLoadoutContentsMenu(
    private val kit: Kit,
    private val loadout: Loadout,
    private val practiceProfile: PracticeProfile
) : Menu()
{
    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for (int in 0 until 27)
        {
            buttons[int] = PaginatedMenu.PLACEHOLDER
        }

        buttons[11] = ItemBuilder
            .of(Material.WOOL)
            .data(13)
            .name("${CC.B_GREEN}Save Loadout")
            .addToLore(
                "${CC.GRAY}Save your current inventory",
                "${CC.GRAY}as the loadout's contents.",
                "",
                "${CC.GREEN}Click to save!"
            )
            .toButton { _, _ ->
                handleLoadoutSave(player).thenRun {
                    handleBackwardsMenuNavigation(player)
                }
            }

        buttons[13] = ItemBuilder
            .of(Material.WOOL)
            .data(4)
            .name("${CC.B_YELLOW}Reset Loadout")
            .addToLore(
                "${CC.GRAY}Reset the loadout to it's",
                "${CC.GRAY}default contents.",
                "",
                "${CC.GREEN}Click to reset loadout!"
            )
            .toButton { _, _ ->
                // handle player inventory reset first
                player.inventory.contents = kit.contents
                player.updateInventory()

                // then handle saving
                for (int in 0 until 36)
                {
                    val defaultContent = kit.contents[int]

                    loadout.inventoryContents[int] = defaultContent
                }

                loadout.timestamp = System.currentTimeMillis()

                val kitLoadouts = practiceProfile.customLoadouts[kit.id]!!
                kitLoadouts[loadout.name] = loadout

                practiceProfile.save().thenRun {
                    player.sendMessage("${CC.GREEN}You have reset this loadout's content.")
                }
            }

        buttons[15] = ItemBuilder
            .of(Material.WOOL)
            .data(14)
            .name("${CC.B_RED}Cancel Edit")
            .addToLore(
                "${CC.GRAY}Cancel the loadout editing",
                "${CC.GRAY}process and return to the",
                "${CC.GRAY}main menu.",
                "",
                "${CC.GREEN}Click to cancel!"
            )
            .toButton { _, _ ->
                handleBackwardsMenuNavigation(player)
            }

        return buttons
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            //save active loadout
            handleLoadoutSave(player).thenRun {
                player.sendMessage("${CC.GREEN}Saving loadout...")

                //revert user to previous state
                Tasks.sync {
                    resetInventory(player)
                    handleBackwardsMenuNavigation(player)
                }
            }
        }
    }

    override fun onOpen(player: Player)
    {
        val inventory = kit.contents

        player.inventory.contents = inventory
        player.updateInventory()
    }

    override fun size(buttons: Map<Int, Button>): Int = 27
    override fun getTitle(player: Player): String = "Editing '${loadout.name}'"

    private fun resetInventory(player: Player)
    {
        player.inventory.clear()
        player.updateInventory()

        LobbyHotbarService.reset(player)
    }

    private fun handleLoadoutSave(player: Player) : CompletableFuture<Void>
    {
        for (i in 0 until 36)
        {
            val edited = player.inventory.getItem(i)

            loadout.inventoryContents[i] = edited
        }

        loadout.timestamp = System.currentTimeMillis()

        val kitLoadouts = practiceProfile.customLoadouts[kit.id]!!

        kitLoadouts[loadout.name] = loadout

        return practiceProfile.save()
    }

    private fun handleBackwardsMenuNavigation(player: Player)
    {
        val loadouts = practiceProfile.getLoadoutsFromKit(kit)

        resetInventory(player)

        if (loadouts.size == 0)
        {
            EditorKitSelectionMenu(practiceProfile).openMenu(player)
        } else
        {
            SelectCustomKitMenu(
                practiceProfile,
                loadouts,
                kit
            ).openMenu(player)
        }
    }
}
