package gg.tropic.practice.menu

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateStatsMenu
import gg.tropic.practice.profile.PracticeProfile
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

/**
 * @author Elb1to
 * @since 10/19/2023
 */
class StatsMenu(
    private val profile: PracticeProfile
) : TemplateStatsMenu()
{
    override fun itemDescriptionOf(player: Player, kit: Kit) = listOf(
        "${CC.B_GREEN} Unranked:",
        "${CC.WHITE}  Wins: ${CC.PRI}${profile.getCasualStatsFor(kit).wins}",
        "${CC.WHITE}  Played: ${CC.PRI}${profile.getCasualStatsFor(kit).plays}",
        "",
        "${CC.WHITE}  Daily Streak: ${CC.PRI}${profile.getCasualStatsFor(kit).dailyStreak.get()}",
        "${CC.WHITE}  Current Streak: ${CC.PRI}${profile.getCasualStatsFor(kit).streak}",
        "${CC.WHITE}  Longest Streak: ${CC.PRI}${profile.getCasualStatsFor(kit).longestStreak}",
        "",
        "${CC.B_GREEN} Ranked:",
        "${CC.WHITE}  ELO: ${CC.PRI}${profile.getRankedStatsFor(kit).elo}",
        "${CC.WHITE}   Peak: ${CC.PRI}${profile.getRankedStatsFor(kit).highestElo}",
        "${CC.WHITE}  Wins: ${CC.PRI}${profile.getRankedStatsFor(kit).wins}"
    )
}
