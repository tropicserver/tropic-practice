package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.reports.GameReportService
import gg.tropic.practice.reports.menu.PlayerViewMenu
import gg.tropic.practice.reports.menu.SelectPlayerMenu
import net.evilblock.cubed.util.CC
import java.util.*

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
@AutoRegister
object MatchInventoryCommand : ScalaCommand()
{
    @CommandAlias("matchinventory")
    fun onInventory(
        player: ScalaPlayer,
        matchId: UUID,
        @Optional playerId: UUID?
    ) = GameReportService.loadSnapshot(matchId)
        .thenAccept {
            if (it == null)
            {
                throw ConditionFailedException(
                    "No match report exists with the ID ${CC.YELLOW}$matchId${CC.RED}."
                )
            }

            if (playerId == null)
            {
                SelectPlayerMenu(it).openMenu(player.bukkit())
                return@thenAccept
            }

            val snapshot = it.snapshots[playerId]
                ?: throw ConditionFailedException(
                    "No match snapshot exists for the player ${CC.YELLOW}${
                        playerId.username()
                    }${CC.RED}."
                )

            PlayerViewMenu(
                gameReport = it,
                snapshot = snapshot
            ).openMenu(
                player.bukkit()
            )
        }
}
