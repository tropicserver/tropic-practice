package gg.tropic.practice.commands.menu

import gg.tropic.practice.map.MapService
import gg.tropic.practice.map.rating.MapRatingService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.Color
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * Class created on 1/12/2024

 * @author 98ping
 * @project tropic-practice
 * @website https://solo.to/redis
 */
class MapRatingOverviewMenu : PaginatedMenu()
{
    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        MapService.cached().maps.values
            .forEach {
                buttons[buttons.size] = ItemBuilder.of(it.displayIcon.type)
                    .name(Color.translate(it.displayName))
                    .addToLore(
                        "${CC.GRAY}Average Rating: ${CC.WHITE}${MapRatingService.ratingMap[it.name] ?: 1}"
                    )
                    .toButton()
            }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) = "Viewing Map Ratings..."
}
