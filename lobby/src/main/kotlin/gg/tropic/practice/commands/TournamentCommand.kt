package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.tournaments.TournamentCreationPipeline
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.services.TournamentManagerService

/**
 * @author GrowlyX
 * @since 12/25/2023
 */
@AutoRegister
@CommandAlias("tournament")
@CommandPermission("practice.command.tournament")
object TournamentCommand : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @AssignPermission
    @Subcommand("create")
    @Description("Enter the tournament creation prompt!")
    fun onCreate(player: ScalaPlayer)
    {
        TournamentCreationPipeline.start().openMenu(player.bukkit())
    }

    @AssignPermission
    @Subcommand("join")
    @Description("Join the ongoing tournament!")
    fun onJoin(player: ScalaPlayer) = TournamentManagerService
        .publish(
            "join",
            "player" to player.uniqueId,
            "canBypassMax" to player.bukkit()
                .hasPermission("practice.tournament.bypass-join-restriction")
        )
        .thenRun {
            val lobbyPlayer = LobbyPlayerService.find(player.bukkit())
                ?: return@thenRun

            synchronized(lobbyPlayer.stateUpdateLock) {
                lobbyPlayer.state = PlayerState.InTournament
                lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
            }
        }

    @AssignPermission
    @Subcommand("leave")
    @Description("Leave the ongoing tournament!")
    fun onLeave(player: ScalaPlayer) = TournamentManagerService
        .publish(
            "leave",
            "player" to player.uniqueId
        )
        .thenRun {
            val lobbyPlayer = LobbyPlayerService.find(player.bukkit())
                ?: return@thenRun

            synchronized(lobbyPlayer.stateUpdateLock) {
                lobbyPlayer.state = PlayerState.Idle
                lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
            }
        }

    @AssignPermission
    @Subcommand("end")
    @Description("End the ongoing tournament!")
    fun onEnd(player: ScalaPlayer) = TournamentManagerService
        .publish(
            "end",
            "player" to player.uniqueId
        )

    @AssignPermission
    @Subcommand("force-start")
    @Description("Force-start the ongoing tournament!")
    fun onForceStart(player: ScalaPlayer) = TournamentManagerService
        .publish(
            "force-start",
            "player" to player.uniqueId
        )
}
