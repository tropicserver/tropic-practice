package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.services.GameManagerService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent

/**
 * @author GrowlyX
 * @since 12/24/2023
 */
@AutoRegister
object MatchInfoCommand : ScalaCommand()
{
    @CommandAlias("matchinfo")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.matchinfo")
    fun onMatchInfo(player: ScalaPlayer, target: AsyncLemonPlayer) = target
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

                    player.sendMessage(
                        "${CC.GREEN}${it.name}'s Match:",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Server: ${CC.GREEN}${reference.server}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} State: ${CC.GREEN}${reference.state.name}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Queue: ${CC.GREEN}${
                            if (reference.queueId == null) "${CC.RED}Private" else reference.queueId
                        }",
                        "",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Map: ${CC.GREEN}${reference.mapID}",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Kit: ${CC.GREEN}${reference.kitID}",
                        "",
                        "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.B_WHITE} Spectators:${
                            if (reference.spectators.isEmpty()) "${CC.RED} None!" else ""
                        }"
                    )

                    if (reference.spectators.isNotEmpty())
                    {
                        reference.spectators.forEach { spectator ->
                            player.sendMessage("  ${CC.GRAY}${Constants.SMALL_DOT_SYMBOL} ${CC.WHITE}${spectator.username()}")
                        }
                    }

                    player.sendMessage(
                        FancyMessage()
                            .withMessage("\n${CC.B_GREEN}(Click to spectate)")
                            .andHoverOf("${CC.GREEN}Click to spectate!")
                            .andCommandOf(
                                ClickEvent.Action.RUN_COMMAND,
                                "/spectate ${it.name}"
                            ),
                    )

                    if (player.bukkit().hasPermission("practice.command.terminatematch"))
                    {
                        player.sendMessage(
                            FancyMessage()
                                .withMessage("${CC.B_RED}(Click to terminate)")
                                .andHoverOf("${CC.RED}Click to terminate!")
                                .andCommandOf(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/terminatematch ${it.name}"
                                )
                        )
                    }
                }
        }
}
