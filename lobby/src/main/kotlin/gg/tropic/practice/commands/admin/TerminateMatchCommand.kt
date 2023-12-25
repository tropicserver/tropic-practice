package gg.tropic.practice.commands.admin

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.scala.lemon.util.QuickAccess
import gg.tropic.practice.games.GameState
import gg.tropic.practice.services.GameManagerService
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 12/24/2023
 */
@AutoRegister
object TerminateMatchCommand : ScalaCommand()
{
    @CommandAlias("terminatematch")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.terminatematch")
    fun onTerminateMatch(player: ScalaPlayer, target: AsyncLemonPlayer) = target
        .validatePlayers(player.bukkit(), false) {
            val online = QuickAccess
                .online(it.uniqueId)
                .join()

            if (!online)
            {
                throw ConditionFailedException(
                    "The player ${CC.YELLOW}${it.name} is not logged onto the network."
                )
            }

            GameManagerService.allGames()
                .thenApplyAsync { games ->
                    games.firstOrNull { game -> it.uniqueId in game.players }
                }
                .thenAccept { reference ->
                    if (reference == null)
                    {
                        throw ConditionFailedException(
                            "The player ${CC.YELLOW}${it.name}${CC.RED} is not currently in a match."
                        )
                    }

                    if (reference.state != GameState.Playing)
                    {
                        throw ConditionFailedException(
                            "You are unable to terminate ${CC.YELLOW}${it.name}'s${CC.RED} match when it is in the ${CC.B_RED}${reference.state.name}${CC.RED} state."
                        )
                    }

                    AwareMessage
                        .of(
                            "terminate",
                            ScalaCommonsSpigot.instance.aware,
                            "server" to reference.server,
                            "matchID" to reference.uniqueId,
                            "terminator" to player.uniqueId
                        )
                        .publish(
                            AwareThreadContext.SYNC,
                            channel = "practice:communications"
                        )

                    player.sendMessage("${CC.GREEN}Sent the match termination request!")
                }
        }
}
