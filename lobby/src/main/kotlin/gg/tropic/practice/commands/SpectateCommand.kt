package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import java.util.concurrent.CompletableFuture

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object SpectateCommand : ScalaCommand()
{
    @CommandAlias("spec|spectate")
    @CommandCompletion("@players")
    fun onSpectate(
        player: ScalaPlayer,
        target: AsyncLemonPlayer
    ): CompletableFuture<Void>
    {
        return target.validatePlayers(player.bukkit(), false) {
        }
    }
}
