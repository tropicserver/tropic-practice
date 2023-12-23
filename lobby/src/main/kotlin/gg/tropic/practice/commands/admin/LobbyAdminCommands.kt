package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.configuration.LobbyConfigurationService
import gg.tropic.practice.map.metadata.anonymous.toPosition
import net.evilblock.cubed.menu.menus.TextEditorMenu
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
@AutoRegister
@CommandAlias("practicelobbyadmin|pla")
@CommandPermission("practice.lobby.commands.admin")
object LobbyAdminCommands : ScalaCommand()
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @AssignPermission
    @Subcommand("export-player-list")
    @Description("Export a list of MIP players.")
    fun onExportPlayerList(player: ScalaPlayer) = CompletableFuture
        .supplyAsync {
            ServerContainer
                .getServersInGroupCasted<GameServer>("mip")
                .flatMap {
                    it.getPlayers()!!
                }
        }
        .thenAccept {
            val export = File(plugin.dataFolder, "player-list.json")
            if (export.exists())
            {
                export.delete()
            }

            export.writeText(Serializers.gson.toJson(it))
            player.sendMessage("${CC.GREEN}Exported player list!")
        }

    @AssignPermission
    @Subcommand("login-motd")
    @Description("Update the login MOTD lines.")
    fun onLoginMOTDLines(player: ScalaPlayer) = with(LobbyConfigurationService.cached()) {
        with(
            object : TextEditorMenu(loginMOTD)
            {
                override fun getPrePaginatedTitle(player: Player) =
                    "Editing: Login MOTD"

                override fun onClose(player: Player)
                {

                }

                override fun onSave(player: Player, list: List<String>)
                {
                    loginMOTD.clear()
                    loginMOTD.addAll(list)

                    LobbyConfigurationService.sync(this@with)
                    player.sendMessage("${CC.GREEN}Saved login MOTD text!")
                }
            }
        ) {
            openMenu(player.bukkit())
        }
    }

    @AssignPermission
    @Subcommand("toggle-ranked-queue")
    @Description("Enable or disable the ability to join ranked queues.")
    fun onToggleRankedQueue(player: ScalaPlayer)
    {
        with(LobbyConfigurationService.cached()) {
            rankedQueueEnabled = !rankedQueueEnabled
            if (rankedQueueEnabled)
            {
                player.sendMessage(
                    "${CC.GREEN}Players are now able to join ranked queues."
                )
            } else
            {
                player.sendMessage(
                    "${CC.RED}Players are now unable to join ranked queues."
                )
            }

            LobbyConfigurationService.sync(this)
        }

    }

    @AssignPermission
    @Subcommand("setspawn")
    @Description("Sets the spawn location for all practice lobby servers.")
    fun onSetSpawn(player: ScalaPlayer)
    {
        val location = player.bukkit().location

        val normalized = location.clone()
        normalized.x = location.x.toInt() + 0.500
        normalized.z = location.z.toInt() + 0.500

        with(LobbyConfigurationService.cached()) {
            spawnLocation = normalized.toPosition()
            LobbyConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the spawn location of the server."
        )
    }
}
