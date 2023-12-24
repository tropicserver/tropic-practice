package gg.tropic.practice.menu.tournaments

import gg.scala.flavor.service.Service
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@Service
object TournamentCreationPipeline
{
    fun start(player: Player) = object : Menu()
    {
        init
        {
            placeholder = true
        }

        private fun teamSizeOf(size: Int) = ItemBuilder
            .of(Material.PAPER)
            .name("${CC.PRI}${size}v${size}")
            .toButton { _, _ ->

            }

        override fun size(buttons: Map<Int, Button>) = 27
        override fun getButtons(player: Player) = mutableMapOf(
            10 to teamSizeOf(1),
            11 to teamSizeOf(2),
            12 to teamSizeOf(3),)

    }.openMenu(player)
}
