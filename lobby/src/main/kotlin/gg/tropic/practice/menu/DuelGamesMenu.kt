package gg.tropic.practice.menu

import gg.tropic.practice.feature.GameReportFeature
import gg.tropic.practice.games.GameReport
import gg.tropic.practice.games.GameReportStatus
import gg.scala.lemon.util.QuickAccess.username
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.time.TimeUtil
import org.apache.commons.lang3.time.DurationFormatUtils
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class DuelGamesMenu(
    private val reports: List<GameReport>
) : PaginatedMenu()
{
    init
    {
        placeholdBorders = true
    }

    override fun size(buttons: Map<Int, Button>) = 36

    override fun getAllPagesButtonSlots() =
        listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
        )

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for (report in this.reports.sortedByDescending { it.matchDate.time })
        {
            if (!report.viewed)
            {
                report.viewed = true

                GameReportFeature.connection.async().set(
                    "duels:snapshots:matches:${report.identifier}",
                    Serializers.gson.toJson(report)
                )
            }

            buttons[buttons.size] = ItemBuilder
                .of(Material.PAPER)
                .name("${CC.GOLD}${
                    TimeUtil.formatIntoFullCalendarString(report.matchDate)
                }")
                .addToLore(
                    "${CC.D_GRAY}(${
                        when (report.status)
                        {
                            GameReportStatus.ForcefullyClosed -> "Forcefully ended"
                            GameReportStatus.Completed -> "Ended normally"
                        }
                    })",
                    "",
                    "${CC.GREEN}Winner${
                        if (report.winners.size == 1) "" else "s"
                    }: ${CC.WHITE}${
                        if (report.winners.isEmpty()) "N/A" else 
                            report.winners.joinToString(", ") {
                                it.username()
                            }
                    }",
                    "${CC.RED}Loser${
                        if (report.losers.size == 1) "" else "s"
                    }: ${CC.WHITE}${
                        if (report.losers.isEmpty()) "N/A" else 
                            report.losers.joinToString(", ") {
                                it.username()
                            }
                    }",
                    "",
                    "${CC.GRAY}Duration: ${CC.WHITE}${
                        DurationFormatUtils
                            .formatDuration(
                                report.duration, "mm:ss"
                            )
                    }",
                    "${CC.GRAY}Arena: ${CC.WHITE}${report.arena}",
                    "",
                    "${CC.GREEN}Click to view inventories!"
                )
                .toButton { _, _ ->
                    SelectPlayerMenu(report).openMenu(player)
                }
        }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) = "Your previous games (last 3d)"
}
