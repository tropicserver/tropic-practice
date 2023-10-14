package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Conditions
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.tropic.practice.reports.GameReportService
import gg.tropic.practice.menu.DuelGamesMenu
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@AutoRegister
@CommandAlias("games|matchhistory")
object DuelGamesCommand : ScalaCommand()
{
    @Default
    fun onDefault(
        @Conditions("cooldown:duration=10,unit=SECONDS")
        player: Player
    ): CompletableFuture<Void>
    {
        return GameReportService
            .loadSnapshotsForParticipant(player.uniqueId)
            .thenAccept {
                Tasks.sync {
                    DuelGamesMenu(it).openMenu(player)
                }
            }
    }
}
