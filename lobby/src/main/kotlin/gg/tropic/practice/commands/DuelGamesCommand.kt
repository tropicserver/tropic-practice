package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.tropic.practice.feature.GameReportFeature
import gg.tropic.practice.menu.DuelGamesMenu
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@AutoRegister
@CommandAlias("games")
object DuelGamesCommand : ScalaCommand()
{
    @Default
    fun onDefault(player: Player): CompletableFuture<Void>
    {
        return GameReportFeature
            .loadSnapshotsForParticipant(player.uniqueId)
            .thenAccept {
                Tasks.sync {
                    DuelGamesMenu(it).openMenu(player)
                }
            }
    }
}
