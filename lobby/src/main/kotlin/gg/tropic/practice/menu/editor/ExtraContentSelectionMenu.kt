package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player

class ExtraContentSelectionMenu(
    private val kit: Kit,
    private val _contentsMenu: EditLoadoutContentsMenu,
    private val contentsMenu: EditLoadoutContentsMenu = EditLoadoutContentsMenu(kit, _contentsMenu.loadout, _contentsMenu.practiceProfile)
) : Menu(
    "Selecting extra contents..."
), AllowRemoveItemsWithinInventory
{
    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        for ((index, content) in kit.additionalContents.withIndex())
        {
            if (content == null)
            {
                continue
            }

            buttons[index] = ItemBuilder
                .copyOf(content)
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    player.inventory.addItem(content)
                }
        }

        return buttons
    }

    override fun onOpen(player: Player)
    {
        super.onOpen(player)
        contentsMenu.onOpen(player)
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.sync {
                contentsMenu.handleLoadoutSave(player)
                    .thenRun {
                        contentsMenu.openMenu(player)
                    }
            }
        }
    }
}
