package gg.tropic.practice.commands.admin

import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.map.metadata.anonymous.toPosition
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.practiceGroup
import gg.tropic.practice.region.Region
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.menu.menus.TextEditorMenu
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
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

    @CommandPermission("op")
    @Subcommand("request-application-reboot")
    @Description("Request an application reboot.")
    fun onRequestAppReboot(player: ScalaPlayer)
    {
        LobbyPlayerService.createMessage("reboot-app")
            .publish(
                AwareThreadContext.ASYNC,
                channel = "practice:application".suffixWhenDev()
            )

        player.sendMessage("${CC.GREEN}We've requested an application reboot, please wait a moment.")
    }

    @CommandPermission("op")
    @Subcommand("restrict-game-regions")
    @Description("Restrict games to a particular region.")
    fun onRequestRegionRestriction(
        player: ScalaPlayer, @Optional region: Region?
    )
    {
        LobbyPlayerService
            .createMessage(
                "force-specific-region",
                "region-id" to (region?.name ?: "__RESET__")
            )
            .publish(
                AwareThreadContext.ASYNC,
                channel = "practice:queue".suffixWhenDev()
            )

        player.sendMessage(
            "${CC.GREEN}We've requested a region restriction to: ${CC.WHITE}${region ?: "Unrestricted"}."
        )
    }

    @AssignPermission
    @Subcommand("export-player-list")
    @Description("Export a list of MIP players.")
    fun onExportPlayerList(player: ScalaPlayer) = CompletableFuture
        .supplyAsync {
            ServerContainer
                .getServersInGroupCasted<GameServer>(practiceGroup().suffixWhenDev())
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
    fun onLoginMOTDLines(player: ScalaPlayer) = with(PracticeConfigurationService.cached()) {
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

                    PracticeConfigurationService.sync(this@with)
                    player.sendMessage("${CC.GREEN}Saved login MOTD text!")
                }
            }
        ) {
            openMenu(player.bukkit())
        }
    }

    @AssignPermission
    @Subcommand("set blockedhits")
    @Description("Edit the Blocked Hit cap for Matches.")
    fun onSetBlockedHits(player: ScalaPlayer, blockedhits: Int)
    {
        with(PracticeConfigurationService.cached()) {
            blockedHitCap = blockedhits
            PracticeConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}Global Blocked Hits limit has been set to: ${CC.PRI}$blockedhits"
        )
    }

    @AssignPermission
    @Subcommand("toggle-ranked-queue")
    @Description("Enable or disable the ability to join ranked queues.")
    fun onToggleRankedQueue(player: ScalaPlayer)
    {
        with(PracticeConfigurationService.cached()) {
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

            PracticeConfigurationService.sync(this)
        }
    }

    @Subcommand("sampling config")
    @Description("Viewing the sampling config.")
    fun onSamplingViewConfig(player: ScalaPlayer)
    {
        with(PracticeConfigurationService.cached()) {
            with(dataSampleThresholds()) {
                player.sendMessage(
                    "${CC.GREEN}Sampling Config:",
                    "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}AutoClick: ${CC.PRI}$autoClick",
                    "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}DoubleClick: ${CC.PRI}$doubleClick",
                )
            }
        }
    }

    @AssignPermission
    @Subcommand("sampling autoclick")
    @Description("Edit the auto click sampling threshold for autobans.")
    fun onSamplingAutoClick(player: ScalaPlayer, requiredMedian: Int)
    {
        with(PracticeConfigurationService.cached()) {
            dataSampleThresholds().autoClick = requiredMedian
            PracticeConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the AutoClick sampling threshold to: ${CC.WHITE}$requiredMedian"
        )
    }

    @AssignPermission
    @Subcommand("sampling doubleclick")
    @Description("Edit the double click sampling threshold for autobans.")
    fun onSamplingDoubleClick(player: ScalaPlayer, requiredMedian: Int)
    {
        with(PracticeConfigurationService.cached()) {
            dataSampleThresholds().doubleClick = requiredMedian
            PracticeConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the DoubleClick sampling threshold to: ${CC.WHITE}$requiredMedian"
        )
    }

    @AssignPermission
    @Subcommand("toggle-synced-tablist")
    @Description("Enable or disable the ability for newly logged in players to see the global tablist.")
    fun onToggleGlobalTabList(player: ScalaPlayer)
    {
        with(PracticeConfigurationService.cached()) {
            enableMIPTabHandler = !enableMIPTabHandler()
            if (enableMIPTabHandler())
            {
                player.sendMessage(
                    "${CC.GREEN}Players are now able to see the global tablist."
                )
            } else
            {
                player.sendMessage(
                    "${CC.RED}Players are now unable to see the global tablist."
                )
            }

            PracticeConfigurationService.sync(this)
        }
    }

    @AssignPermission
    @Subcommand("ranked-queue-minimum-requirement")
    @Description("Edit the number of wins required to queue for a ranked kit.")
    fun onEditRankedMinimumRequirement(player: ScalaPlayer, requirement: Int)
    {
        with(PracticeConfigurationService.cached()) {
            rankedMinimumWinRequirement = requirement
            PracticeConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the minimum win requirement to: ${CC.B_WHITE}$requirement"
        )
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

        with(PracticeConfigurationService.cached()) {
            spawnLocation = normalized.toPosition()
            PracticeConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the spawn location of the server."
        )
    }
}
