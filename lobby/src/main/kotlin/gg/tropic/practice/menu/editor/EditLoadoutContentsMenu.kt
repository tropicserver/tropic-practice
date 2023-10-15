package gg.tropic.practice.menu.editor

import com.cryptomorin.xseries.XMaterial
import gg.scala.lemon.filter.ChatMessageFilterHandler
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.player.hotbar.LobbyHotbarService
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.menus.ConfirmMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class EditLoadoutContentsMenu(
    private val kit: Kit,
    val loadout: Loadout,
    val practiceProfile: PracticeProfile
) : Menu()
{
    init
    {
        placeholder = true
    }

    private var movingToExtendedContentMenu = false
    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val hasExtraContents = kit.additionalContents.isNotEmpty() &&
            kit.additionalContents.any { it != null }

        buttons[11] = ItemBuilder
            .of(XMaterial.LIME_WOOL)
            .name("${CC.B_GREEN}Save Loadout")
            .addToLore(
                "${CC.WHITE}Save your current inventory",
                "${CC.WHITE}as the loadout's contents.",
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
                "${CC.WHITE}Reset the loadout to it's",
                "${CC.WHITE}default contents.",
                "",
                "${CC.YELLOW}Click to reset loadout!"
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

                practiceProfile.save().thenRun {
                    player.sendMessage("${CC.GREEN}You have reset this loadout's content.")
                }
            }

        buttons[15] = ItemBuilder
            .of(Material.WOOL)
            .data(14)
            .name("${CC.B_RED}Cancel Edit")
            .addToLore(
                "${CC.WHITE}Cancel the loadout editing",
                "${CC.WHITE}process and return to the",
                "${CC.WHITE}main menu.",
                "",
                "${CC.RED}Click to cancel!"
            )
            .toButton { _, _ ->
                handleBackwardsMenuNavigation(player)
            }

        buttons[if (!hasExtraContents) 21 else 20] = ItemBuilder
            .of(XMaterial.NAME_TAG)
            .name("${CC.B_GREEN}Edit Name")
            .addToLore(
                "",
                "${CC.GREEN}Click to edit name!"
            )
            .toButton { _, _ ->
                player.closeInventory()
                player.sendMessage("${CC.GREEN}Type the new name of the loadout in chat.")
                player.sendMessage("${CC.GREEN}Type ${CC.RED}cancel ${CC.GREEN}to cancel.")
                player.sendMessage("${CC.GRAY}(chat colors are supported)")

                InputPrompt()
                    .acceptInput { _, input ->
                        if (input.equals("cancel", true))
                        {
                            player.sendMessage("${CC.RED}You have cancelled the name edit.")
                            openMenu(player)
                            return@acceptInput
                        }

                        if (ChatMessageFilterHandler.handleMessageFilter(player, input, reportToStaff = false))
                        {
                            player.sendMessage("${CC.RED}Your custom loadout name contains a word that is not allowed!")
                            openMenu(player)
                            return@acceptInput
                        }

                        loadout.name = ChatColor.translateAlternateColorCodes('&', input)
                        practiceProfile.save().thenRun {
                            player.sendMessage("${CC.GREEN}You have changed the name of this loadout to: ${loadout.name}${CC.GREEN}.")
                            openMenu(player)
                        }
                    }
                    .start(player)
            }

        buttons[if (!hasExtraContents) 23 else 22] = ItemBuilder
            .of(XMaterial.RED_DYE)
            .name("${CC.B_RED}Delete")
            .addToLore(
                "",
                "${CC.RED}Click to delete!"
            )
            .toButton { _, _ ->
                ConfirmMenu(
                    title = "Deleting loadout: ${loadout.name}",
                    confirm = true
                ) {
                    if (it)
                    {
                        practiceProfile.customLoadouts[kit.id]
                            ?.remove(loadout)

                        practiceProfile.save().thenRun {
                            player.sendMessage(
                                "${CC.GREEN}You have just deleted your ${CC.YELLOW}${loadout.name} ${CC.GREEN}loadout for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
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
                    } else
                    {
                        openMenu(player)
                    }
                }.openMenu(player)
            }

        if (hasExtraContents)
        {
            buttons[24] = ItemBuilder
                .of(Material.CHEST)
                .name("${CC.BD_AQUA}Additional Contents")
                .addToLore(
                    "${CC.WHITE}View the additional contents",
                    "${CC.WHITE}of this kit.",
                    "",
                    "${CC.AQUA}Click to view!"
                )
                .toButton { _, _ ->
                    movingToExtendedContentMenu = true

                    handleLoadoutSave(player).thenRun {
                        ExtraContentSelectionMenu(kit, this).openMenu(player)
                    }
                }
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
                handleBackwardsMenuNavigation(player)
            }
        }

        //revert user to previous state
        if (!movingToExtendedContentMenu)
        {
            Tasks.sync {
                resetInventory(player)
            }
        }
    }

    override fun onOpen(player: Player)
    {
        val inventory = loadout.inventoryContents
        player.inventory.contents = inventory
        player.updateInventory()
    }

    override fun size(buttons: Map<Int, Button>): Int = 36
    override fun getTitle(player: Player): String = "Editing loadout: ${loadout.name}"

    private fun resetInventory(player: Player)
    {
        player.inventory.clear()
        player.updateInventory()

        LobbyHotbarService.reset(player)
    }

    fun handleLoadoutSave(player: Player) : CompletableFuture<Void>
    {
        for (i in 0 until 36)
        {
            val edited = player.inventory.getItem(i)

            loadout.inventoryContents[i] = edited
        }

        loadout.timestamp = System.currentTimeMillis()

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
