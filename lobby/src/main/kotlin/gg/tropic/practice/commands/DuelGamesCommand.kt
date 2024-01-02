package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.menu.DuelGamesMenu
import gg.tropic.practice.reports.GameReportService
import net.evilblock.cubed.util.bukkit.Tasks
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@AutoRegister
@CommandAlias("games|matchhistory|mh|matchhist")
object DuelGamesCommand : ScalaCommand()
{
    @Default
    @CommandCompletion("@mip-players")
    fun onDefault(
        @Conditions("cooldown:duration=10,unit=SECONDS")
        player: ScalaPlayer,
        @Optional
        @CommandPermission("practice.command.matchhistory.view-other-profiles")
        target: AsyncLemonPlayer?
    ): CompletableFuture<Void>
    {
        if (target == null)
        {
            return GameReportService
                .loadSnapshotsForParticipant(player.uniqueId)
                .thenAccept {
                    Tasks.sync {
                        DuelGamesMenu(it, player.uniqueId).openMenu(player.bukkit())
                    }
                }
        }

        return target.validatePlayers(player.bukkit(), false) { lemonPlayer ->
            GameReportService
                .loadSnapshotsForParticipant(lemonPlayer.uniqueId)
                .thenAccept {
                    Tasks.sync {
                        DuelGamesMenu(it, lemonPlayer.uniqueId).openMenu(player.bukkit())
                    }
                }
                .join()
        }
    }
}
