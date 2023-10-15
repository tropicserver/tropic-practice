package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.profile.PracticeProfileService
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 10/14/2023
 */
@AutoRegister
object DuelCommands : ScalaCommand()
{
    @CommandPermission("@players")
    @CommandAlias("duel|fight|duelrequest")
    fun onDuelPlayer(player: ScalaPlayer, target: AsyncLemonPlayer): CompletableFuture<Void>
    {
        return target.validatePlayers(
            player.bukkit(), false
        ) {

        }
    }
}
