package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.StatsMenu
import gg.tropic.practice.profile.PracticeProfileService
import net.evilblock.cubed.util.CC

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object StatsCommand : ScalaCommand()
{
    @CommandAlias("stats|statistics")
    fun onStatsUse(player: ScalaPlayer, target: ScalaPlayer? = null)
    {
        val profile = target?.let {
            PracticeProfileService.find(it.uniqueId)
        }
        if (profile == null)
        {
            player.sendMessage("${CC.RED}That player does not exist.")
            return
        }

        StatsMenu(profile).openMenu(player.bukkit())
    }
}