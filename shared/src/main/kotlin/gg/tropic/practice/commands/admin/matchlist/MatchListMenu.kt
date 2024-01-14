package gg.tropic.practice.commands.admin.matchlist

import gg.scala.lemon.util.QuickAccess.username
import gg.scala.lemon.util.SplitUtil
import gg.tropic.practice.games.GameReference
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.menus.ConfirmMenu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 1/13/2024
 */
class MatchListMenu(
    private val games: List<GameReference>
) : PaginatedMenu()
{
    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        var indexCounter = 0

        return games
            .map { reference ->
                ItemBuilder
                    .of(Material.PAPER)
                    .name(
                        "${CC.GREEN}#${SplitUtil.splitUuid(reference.uniqueId)}"
                    )
                    .addToLore(
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Server: ${CC.GREEN}${reference.server}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} State: ${CC.GREEN}${reference.state.name}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Queue: ${CC.GREEN}${
                            reference.queueId ?: "${CC.RED}Private"
                        }",
                        "",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Map: ${CC.GREEN}${reference.mapID}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Kit: ${CC.GREEN}${reference.kitID}",
                        "",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.B_WHITE} Spectators:${
                            if (reference.spectators.isEmpty()) "${CC.RED} None!" else ""
                        }"
                    )
                    .apply {
                        if (reference.spectators.isNotEmpty())
                        {
                            reference.spectators.forEach { spectator ->
                                addToLore("  ${CC.GRAY}${Constants.SMALL_DOT_SYMBOL} ${CC.WHITE}${spectator.username()}")
                            }
                        }
                    }
                    .addToLore(
                        "",
                        "${CC.GREEN}(Click to spectate)",
                        "${CC.D_RED}(Shift-Click to terminate)"
                    )
                    .toButton { _, type ->
                        if (type!!.isShiftClick)
                        {
                            ConfirmMenu(
                                title = "Confirm match termination",
                                confirm = true,
                                callback = { confirmed ->
                                    if (!confirmed)
                                    {
                                        Tasks.sync { openMenu(player) }
                                        return@ConfirmMenu
                                    }

                                    player.performCommand(
                                        "terminatematch ${reference.players.first().username()}"
                                    )
                                }
                            ).openMenu(player)
                            return@toButton
                        }

                        player.performCommand(
                            "spectate ${reference.players.first().username()}"
                        )
                    }
            }
            .associateBy { indexCounter++ }
    }

    override fun getPrePaginatedTitle(player: Player) = "Viewing all games"
}
