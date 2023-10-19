package gg.tropic.practice.menu

import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.profile.PracticeProfile
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author Elb1to
 * @since 10/19/2023
 */
class StatisticsMenu(
    private val profile: PracticeProfile
) : TemplateKitMenu()
{
    override fun filterDisplayOfKit(player: Player, kit: Kit) = true

    override fun itemDescriptionOf(player: Player, kit: Kit) =
        with(this) {
            val casualStats = profile.getCasualStatsFor(kit)
            val rankedStats = profile.getRankedStatsFor(kit)

            listOf(
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
                "",
                "${CC.WHITE}Daily Streak: ${CC.AQUA}${casualStats.dailyStreak()}",
                "${CC.WHITE}Current Streak: ${CC.AQUA}${casualStats.streak}",
                "${CC.WHITE}Longest Streak: ${CC.AQUA}${casualStats.longestStreak}"
            )
        }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {

    }

    override fun getPrePaginatedTitle(player: Player) = "${profile.identifier.username()}'s statistics"
}
