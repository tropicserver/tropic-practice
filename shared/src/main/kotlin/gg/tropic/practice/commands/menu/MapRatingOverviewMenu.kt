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

        MapService.cached().maps.values.forEach {
            buttons[buttons.size] = ItemBuilder.of(it.displayIcon.type)
                .name(Color.translate(it.displayName))
                .addToLore(
                    "${CC.GRAY}Total Ratings: ${CC.WHITE}${MapRatingService.getRatingCount(it)}",
                    "${CC.GRAY}Average Rating: ${CC.WHITE}${MapRatingService.ratingMap[it.name] ?: 1}",
                    "",
                    "${CC.GREEN}Click to lock this map!"
                ).toButton { _, _ ->
                    with(MapService.cached()) {
                        it.locked = !it.locked
                        MapService.sync(this)
                    }

                    player.sendMessage(
                        "${CC.YELLOW}You have just ${if (it.locked) "${CC.GREEN}locked" else "${CC.RED}unlocked"} ${CC.YELLOW}the map ${
                            Color.translate(
                                it.displayName
                            )
                        }"
                    )
                }
        }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player): String
    {
        return "Viewing Map Ratings..."
    }
}