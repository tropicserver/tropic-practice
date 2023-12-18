package gg.tropic.practice.menu

import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
class PlayerMainMenu : Menu()
{
    init
    {
        placeholder = true
    }

    override fun getButtons(player: Player) = mapOf(
        11 to ItemBuilder
            .of(Material.BLAZE_POWDER)
            .name("${CC.GOLD}Cosmetics")
            .addToLore(
                "${CC.GRAY}Customize everything about",
                "${CC.GRAY}your in-game profile using",
                "${CC.GRAY}the cosmetics menu!",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                player.performCommand("cosmetics")
            },
        13 to ItemBuilder
            .of(Material.LAPIS_ORE)
            .name("${CC.BLUE}Statistics")
            .addToLore(
                "${CC.GRAY}View practice statistics",
                "${CC.GRAY}in all categories!",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                player.performCommand("statistics")
            },
        15 to ItemBuilder
            .of(Material.DIAMOND_BLOCK)
            .name("${CC.AQUA}Tournaments")
            .addToLore(
                "${CC.GRAY}Host or join a tournament!",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                      // TODO: Tournament join
            },
    )
}
