package gg.tropic.practice.menu

import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author Elb1to
 * @since 10/19/2023
 */
class StatisticsMenu(
    private val profile: PracticeProfile,
    private val state: StatisticMenuState
) : TemplateKitMenu()
{
    override fun filterDisplayOfKit(player: Player, kit: Kit) = true

    override fun itemDescriptionOf(player: Player, kit: Kit) =
        with(this) {
            val casualStats = profile.getCasualStatsFor(kit)
            val rankedStats = profile.getRankedStatsFor(kit)
            val globalStats = profile.globalStatistics

            val description = mutableListOf<String>()
            val unrankedLore = listOf(
                "${CC.GREEN}Unranked:",
                "${CC.WHITE}Wins: ${CC.GREEN}${casualStats.wins}",
                "${CC.WHITE}Played: ${CC.GREEN}${casualStats.plays}",
                "",
                "${CC.WHITE}Kills: ${CC.GREEN}${casualStats.kills}",
                "${CC.WHITE}Deaths: ${CC.GREEN}${casualStats.deaths}",
                "",
                "${CC.WHITE}Daily Streak: ${CC.GREEN}${casualStats.dailyStreak()}",
                "${CC.WHITE}Current Streak: ${CC.GREEN}${casualStats.streak}",
                "${CC.WHITE}Longest Streak: ${CC.GREEN}${casualStats.longestStreak}",
                "",
            )
            val rankedLore = listOf(
                "${CC.AQUA}Ranked:",
                "${CC.WHITE}Wins: ${CC.AQUA}${rankedStats.wins}",
                "${CC.WHITE}Played: ${CC.AQUA}${rankedStats.wins}",
                "",
                "${CC.WHITE}ELO: ${CC.PRI}${rankedStats.elo}",
                "${CC.WHITE} ${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Peak: ${CC.PRI}${
                    rankedStats.highestElo
                }",
                "",
                "${CC.WHITE}Kills: ${CC.AQUA}${rankedStats.kills}",
                "${CC.WHITE}Deaths: ${CC.AQUA}${rankedStats.deaths}",
            )
            val globalLore = listOf(
                "${CC.GOLD}Global:",
                "${CC.WHITE}Total Wins: ${CC.AQUA}${globalStats.totalWins}",
                "${CC.WHITE}Total Losses: ${CC.AQUA}${globalStats.totalLosses}",
                "${CC.WHITE}Total Played: ${CC.AQUA}${globalStats.totalPlays}",
                "",
                "${CC.WHITE}Total Kills: ${CC.AQUA}${globalStats.totalKills}",
                "${CC.WHITE}Total Deaths: ${CC.AQUA}${globalStats.totalDeaths}",
            )

            when (state)
            {
                StatisticMenuState.Unranked ->
                {
                    description.addAll(
                        unrankedLore
                    )
                }

                StatisticMenuState.Ranked ->
                {
                    description.addAll(
                        rankedLore
                    )
                }

                StatisticMenuState.Global ->
                {
                    description.addAll(
                        globalLore
                    )
                }
            }

            description
        }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {

    }

    override fun getGlobalButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[40] = ItemBuilder.of(Material.PAPER)
            .name("${CC.B_YELLOW}Global Stats")
            .setLore(
                listOf(
                    " ",
                    "${CC.YELLOW}Click to view"
                )
            ).toButton { _, _ ->
                StatisticsMenu(
                    profile,
                    StatisticMenuState.Global
                ).openMenu(player)
            }

        buttons[39] = ItemBuilder.of(Material.CARPET)
            .name("${CC.B_GREEN}Casual Statistics")
            .data(5)
            .setLore(
                listOf(
                    " ",
                    "${CC.YELLOW}Click to view"
                )
            ).toButton { _, _ ->
                StatisticsMenu(
                    profile,
                    StatisticMenuState.Unranked
                ).openMenu(player)
            }

        buttons[41] = ItemBuilder.of(Material.CARPET)
            .name("${CC.B_AQUA}Ranked Statistics")
            .data(3)
            .setLore(
                listOf(
                    " ",
                    "${CC.YELLOW}Click to view"
                )
            ).toButton { _, _ ->
                StatisticsMenu(
                    profile,
                    StatisticMenuState.Ranked
                ).openMenu(player)
            }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) = "${profile.identifier.username()}'s statistics"

    enum class StatisticMenuState
    {
        Global, Ranked, Unranked,
    }
}