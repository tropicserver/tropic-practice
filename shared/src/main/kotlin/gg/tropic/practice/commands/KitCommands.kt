package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import net.evilblock.cubed.util.CC
import java.util.*

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
@AutoRegister
@CommandAlias("kit")
@CommandPermission("practice.command.kit")
object KitCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    /*@AssignPermission
    @Subcommand("delete")
    @CommandCompletion("@kits")
    @Description("Delete an existing kit.")
    fun onDelete(player: ScalaPlayer, kit: Kit)
    {
        // TODO: ensure no matches are ongoing with this kit

    }*/

    @AssignPermission
    @Subcommand("create")
    @CommandCompletion("@kits")
    @Description("Create a new kit.")
    fun onCreate(player: ScalaPlayer, @Single id: String)
    {
        val lowercaseID = id.lowercase()

        // TODO: ensure no matches are ongoing with this kit

        if (KitService.cached().kits[lowercaseID] != null)
        {
            throw ConditionFailedException(
                "A kit with the ID ${CC.YELLOW}$lowercaseID${CC.RED} already exists."
            )
        }

        val kit = Kit(
            id = lowercaseID,
            displayName = id
                .replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.getDefault())
                    else
                        it.toString()
                }
        )

        with(KitService.cached()) {
            kits[lowercaseID] = kit
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You created a new kit with the ID ${CC.YELLOW}$lowercaseID${CC.GREEN}."
        )
    }
}
