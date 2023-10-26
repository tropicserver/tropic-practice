package gg.tropic.practice.menu

import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.profile.PracticeProfile
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

    override fun itemTitleFor(player: Player, kit: Kit) = if (state == StatisticMenuState.Casual)
    {
        "${CC.B_GREEN}${kit.displayName}"
    } else
    {
        "${CC.B_AQUA}${kit.displayName}"
    }

    override fun itemDescriptionOf(player: Player, kit: Kit) =
        with(this) {
            val casualStats = profile.getCasualStatsFor(kit)
            val rankedStats = profile.getRankedStatsFor(kit)
            val unrankedLore = listOf(
                "${CC.WHITE}Wins: ${CC.GREEN}${casualStats.wins}",
                "${CC.WHITE}Losses: ${CC.GREEN}${casualStats.plays - casualStats.wins}",
                "${CC.WHITE}Played: ${CC.GREEN}${casualStats.plays}",
                "",
                "${CC.WHITE}Kills: ${CC.GREEN}${casualStats.kills}",
                "${CC.WHITE}Deaths: ${CC.GREEN}${casualStats.deaths}",
                "",
                "${CC.GREEN}Streaks:",
                "${CC.WHITE}Daily: ${CC.GREEN}${casualStats.dailyStreak()}",
                "${CC.WHITE}Current: ${CC.GREEN}${casualStats.streak}",
                "${CC.WHITE}Peak: ${CC.GREEN}${casualStats.longestStreak}"
            )

            val rankedLore = listOf(
                "${CC.WHITE}Wins: ${CC.AQUA}${rankedStats.wins}",
                "${CC.WHITE}Losses: ${CC.AQUA}${rankedStats.plays - rankedStats.wins}",
                "${CC.WHITE}Played: ${CC.AQUA}${rankedStats.plays}",
                "",
                "${CC.WHITE}Kills: ${CC.AQUA}${rankedStats.kills}",
                "${CC.WHITE}Deaths: ${CC.AQUA}${rankedStats.deaths}",
                "",
                "${CC.WHITE}ELO: ${CC.AQUA}${rankedStats.elo} ${CC.GRAY}(#1)",
                "${CC.WHITE} ${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Highest: ${CC.AQUA}${
                    rankedStats.highestElo
                }",
                "",
                "${CC.AQUA}Streaks:",
                "${CC.WHITE}Daily: ${CC.AQUA}${rankedStats.dailyStreak()}",
                "${CC.WHITE}Current: ${CC.AQUA}${rankedStats.streak}",
                "${CC.WHITE}Peak: ${CC.AQUA}${rankedStats.longestStreak}"
            )


            when (state)
            {
                StatisticMenuState.Casual -> unrankedLore
                StatisticMenuState.Ranked -> rankedLore
            }
        }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {

    }

    override fun getGlobalButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val globalStats = profile.globalStatistics

        buttons[40] = ItemBuilder
            .of(Material.NETHER_STAR)
            .name("${CC.D_AQUA}Global")
            .setLore(
                listOf(
                    "${CC.WHITE}Wins: ${CC.AQUA}${globalStats.totalWins}",
                    "${CC.WHITE}Losses: ${CC.AQUA}${globalStats.totalLosses}",
                    "${CC.WHITE}Played: ${CC.AQUA}${globalStats.totalPlays}",
                    "",
                    "${CC.WHITE}Kills: ${CC.AQUA}${globalStats.totalKills}",
                    "${CC.WHITE}Deaths: ${CC.AQUA}${globalStats.totalDeaths}"
                )
            )
            .toButton()

        buttons[39] = ItemBuilder
            .of(
                if (state == StatisticMenuState.Casual)
                    Material.WOOL else Material.CARPET
            )
            .apply {
                if (state == StatisticMenuState.Casual)
                    glow()
            }
            .name("${CC.B_GREEN}Casual Statistics")
            .data(5)
            .addToLore(
                " ",
                "${CC.GREEN}Click to view!"
            )
            .toButton { _, _ ->
                if (state == StatisticMenuState.Casual)
                    return@toButton

                Button.playNeutral(player)

                StatisticsMenu(
                    profile,
                    StatisticMenuState.Casual
                ).openMenu(player)
            }

        buttons[41] = ItemBuilder
            .of(
                if (state == StatisticMenuState.Ranked)
                    Material.WOOL else Material.CARPET
            )
            .name("${CC.B_AQUA}Ranked Statistics")
            .data(3)
            .apply {
                if (state == StatisticMenuState.Ranked)
                    glow()
            }
            .addToLore(
                " ",
                "${CC.AQUA}Click to view!"
            )
            .toButton { _, _ ->
                if (state == StatisticMenuState.Ranked)
                    return@toButton

                Button.playNeutral(player)

                StatisticsMenu(
                    profile,
                    StatisticMenuState.Ranked
                ).openMenu(player)
            }

        return buttons
    }

    override fun getPrePaginatedTitle(player: Player) =
        "${profile.identifier.username()}'s ${state.name} Statistics"

    enum class StatisticMenuState
    {
        Ranked, Casual
    }
}
