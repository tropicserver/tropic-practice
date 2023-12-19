package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.menu.StatisticsMenu
import gg.tropic.practice.profile.PracticeProfileService
import java.util.concurrent.CompletableFuture

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object StatisticsCommand : ScalaCommand()
{
    @CommandAlias("stats|statistics")
    @CommandCompletion("@mip-players")
    fun onStatistics(
        player: ScalaPlayer,
        @Optional target: AsyncLemonPlayer?
    ): CompletableFuture<Void>
    {
        if (target != null)
        {
            return target.validatePlayers(player.bukkit(), false) {
                StatisticsMenu(
                    it.identifier.offlineProfile,
                    StatisticsMenu.StatisticMenuState.Casual
                ).openMenu(player.bukkit())
            }
        }

        val profile = PracticeProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Your profile has not loaded in properly, log out and try again."
            )

        StatisticsMenu(
            profile,
            StatisticsMenu.StatisticMenuState.Casual
        ).openMenu(player.bukkit())

        return CompletableFuture.completedFuture(null)
    }
}
