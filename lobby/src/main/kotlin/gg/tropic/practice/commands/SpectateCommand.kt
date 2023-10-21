package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.scala.lemon.util.QuickAccess
import gg.tropic.practice.games.SpectateRequest
import gg.tropic.practice.queue.QueueService
import net.evilblock.cubed.util.CC

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object SpectateCommand : ScalaCommand()
{
    @CommandAlias("spectate|spec")
    @CommandCompletion("@players")
    fun onSpectate(
        player: ScalaPlayer,
        target: AsyncLemonPlayer
    ) = target.validatePlayers(
        player.bukkit(), false
    ) {
        it.identifier.offlineProfile
        player.bukkit().sendMessage(
            "${CC.GRAY}Joining ${CC.YELLOW}${it.name}'s${CC.GRAY} game..."
        )

        QueueService.spectate(
            SpectateRequest(
                player.uniqueId,
                it.uniqueId,
                player.bukkit().hasPermission(
                    "practice.spectate.bypass-allowance-settings"
                )
            )
        )
    }
}
