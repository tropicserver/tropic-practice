package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.LeaderboardsMenu

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
@AutoRegister
object LeaderboardsCommand : ScalaCommand()
{
    @CommandAlias("leaderboards|lbs")
    fun onLeaderboards(player: ScalaPlayer)
    {
        LeaderboardsMenu().openMenu(player.bukkit())
    }
}
