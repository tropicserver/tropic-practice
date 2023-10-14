package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.configuration.LobbyConfigurationService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
@CommandAlias("practicelobbyadmin|pla")
@CommandPermission("practice.lobby.commands.admin")
object LobbyAdminCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
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
            spawnLocation = normalized
            LobbyConfigurationService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the spawn location of the server."
        )
    }
}
