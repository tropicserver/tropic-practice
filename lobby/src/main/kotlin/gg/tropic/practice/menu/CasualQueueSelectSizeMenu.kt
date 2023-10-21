package gg.tropic.practice.menu

import com.cryptomorin.xseries.XMaterial
import gg.tropic.practice.games.QueueType
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/15/2023
 */
class CasualQueueSelectSizeMenu : Menu("Casual Queue")
{
    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player) = mutableMapOf(
        12 to ItemBuilder
            .of(XMaterial.ORANGE_DYE)
            .name("${CC.B_GOLD}Solos")
            .addToLore(
                "${CC.WHITE}Queue for a casual",
                "${CC.WHITE}solos game with no",
                "${CC.WHITE}loss penalty.",
                "",
                "${CC.WHITE}Playing: ${CC.GOLD}0",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                Button.playNeutral(player)
                JoinQueueMenu(QueueType.Casual, 1).openMenu(player)
            },
        14 to ItemBuilder
            .of(XMaterial.LIGHT_BLUE_DYE)
            .name("${CC.B_AQUA}Duos")
            .addToLore(
                "${CC.WHITE}Queue for a casual",
                "${CC.WHITE}duos game with no",
                "${CC.WHITE}loss penalty.",
                "",
                "${CC.WHITE}Playing: ${CC.AQUA}0",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                Button.playNeutral(player)
                JoinQueueMenu(QueueType.Casual, 2).openMenu(player)
            }
    )
}
