package gg.tropic.practice.menu.editor

import gg.tropic.practice.utilities.deepClone
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class EditorKitSelectionMenu(
    private val practiceProfile: PracticeProfile
) : TemplateKitMenu()
{
    init
    {
        updateAfterClick = true
    }

    override fun shouldIncludeKitDescription() = false

    override fun filterDisplayOfKit(player: Player, kit: Kit): Boolean = true
    override fun itemTitleFor(player: Player, kit: Kit) = "${CC.B_PRI}${kit.displayName}"

    private val cursorPositions = mutableMapOf<String, Int>()

    override fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    {
        val loadouts = practiceProfile.getLoadoutsFromKit(kit)
        val loadoutList = mutableListOf<String>()

        if (loadouts.isEmpty())
        {
            loadoutList += "${CC.B_RED}None! ${CC.RED}Click to create"
            loadoutList += "${CC.RED}a new loadout."
        } else
        {
            val cursorPosition = cursorPositions.getOrPut(kit.id) { 0 }
            loadoutList += "${CC.WHITE}Your Loadouts:"
            loadoutList += "${CC.I_GRAY}(Right-click to scroll)"

            loadouts.forEachIndexed { index, loadout ->
                loadoutList += "${if (cursorPosition == index) "${CC.GREEN}► " else CC.GRAY}${loadout.name}"
            }

            loadoutList += ""
            loadoutList += "${CC.GREEN}Click to edit!"
            loadoutList += "${CC.D_GREEN}Shift-click to create!"
        }

        return loadoutList
    }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        val loadouts = practiceProfile.getLoadoutsFromKit(kit)

        if (loadouts.isEmpty() || type == ClickType.SHIFT_LEFT)
        {
            if (loadouts.size >= 8)
            {
                player.sendMessage("${CC.RED}You have reached the limit of custom loadouts!")
                return
            }

            val loadout = Loadout(
                name = "Loadout #${loadouts.size + 1}",
                kit.id,
                System.currentTimeMillis(),
                inventoryContents = kit.contents.deepClone()
            )

            practiceProfile.customLoadouts[kit.id]?.add(loadout)

            Button.playNeutral(player)
            EditLoadoutContentsMenu(
                kit, loadout, practiceProfile
            ).openMenu(player)
            return
        }

        if (type == ClickType.LEFT)
        {
            val cursorPosition = cursorPositions.getOrPut(kit.id) { 0 }
            val loadoutAt = loadouts[cursorPosition]
            Button.playNeutral(player)
            EditLoadoutContentsMenu(
                kit, loadoutAt, practiceProfile
            ).openMenu(player)
            return
        }

        if (type == ClickType.RIGHT)
        {
            cursorPositions.compute(kit.id) { _, value ->
                var nextPosition = (value ?: 0) + 1
                if (nextPosition > loadouts.size - 1)
                {
                    nextPosition = 0
                }

                nextPosition
            }
        }
    }

    override fun getPrePaginatedTitle(player: Player) = "Select a kit to edit..."
}
