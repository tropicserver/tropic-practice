package gg.tropic.practice.menu.editor

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.loadout.Loadout
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class EditorKitSelectionMenu(
    private val practiceProfile: PracticeProfile
) : TemplateKitMenu()
{
    override fun filterDisplayOfKit(player: Player, kit: Kit): Boolean = true

    override fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    {
        val loadouts = practiceProfile.getLoadoutsFromKit(kit)

        return mutableListOf(
            "${CC.GRAY}Custom loadouts:",
            "${CC.WHITE}${loadouts.size}/8",
            " "
        ).apply {
            if (loadouts.size != 0)
            {
                this += "${CC.B_GREEN}Shift-click to edit!"
            }
            this += "${CC.GREEN}Click to create new!"
        }
    }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        val loadouts = practiceProfile.getLoadoutsFromKit(kit)

        if (type == ClickType.LEFT)
        {
            if (loadouts.size >= 8)
            {
                player.sendMessage("${CC.RED}You have reached the maximum number of custom kits!")
                return
            }

            val loadout = Loadout(
                name = "Default #${loadouts.size+1}",
                kit.id,
                System.currentTimeMillis()
            )

            EditLoadoutContentsMenu(kit, loadout, practiceProfile).openMenu(player)
        } else if (type == ClickType.SHIFT_LEFT)
        {
            if (loadouts.size != 0)
            {
                SelectCustomKitMenu(
                    practiceProfile,
                    loadouts,
                    kit
                ).openMenu(player)
            }
        }
    }

    override fun getPrePaginatedTitle(player: Player) = "Select a kit to edit..."
}
