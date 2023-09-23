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
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupContainer
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import java.util.*

@AutoRegister
@CommandAlias("kitgroup")
@CommandPermission("practice.command.kitgroup")
object KitGroupCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    @AssignPermission
    @Subcommand("info")
    @CommandCompletion("@kit-groups")
    @Description("Show information about a given kit group.")
    fun onInfo(player: ScalaPlayer, kitGroup: KitGroup)
    {
        val kits = kitGroup.kits()
        player.sendMessage(" ")
        player.sendMessage("${CC.GREEN}Information for the kit group ${CC.WHITE}${kitGroup.id}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}Kits Using Group ${CC.WHITE}(${
            kits.size
        })${CC.GRAY}: ${
            if (kits.isNotEmpty())
                kitGroup.kits().joinToString { "${CC.WHITE}${it.displayName}, " }
            else "${CC.RED}None"
        }")
        player.sendMessage(" ")
    }

    @AssignPermission
    @Subcommand("list")
    @Description("List all kit groups.")
    fun onList(player: ScalaPlayer)
    {
        val listFancyMessage = FancyMessage()
        val kitGroups = KitGroupService.cached().groups

        player.sendMessage(
            "${CC.GREEN}Kit Groups"
        )

        for ((i, group) in kitGroups.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.id}${
                        if (i != (kitGroups.size - 1)) ", " else ""
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view kit group information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kitgroup info ${group.id}"
                )

            listFancyMessage.components.add(uniqueInfoComponent.components[0])
        }

        listFancyMessage.sendToPlayer(player.bukkit())
    }

    @AssignPermission
    @Subcommand("create")
    @CommandCompletion("@kit-groups")
    @Description("Create a new kit group.")
    fun onCreate(player: ScalaPlayer, @Single id: String)
    {
        val lowercaseID = id.lowercase()
        val cachedGroup = KitGroupService.cached().groups
            .firstOrNull { it.id == lowercaseID }

        if (cachedGroup != null)
        {
            throw ConditionFailedException(
                "A kit group with the ID ${CC.YELLOW}$lowercaseID${CC.RED} already exists."
            )
        }

        val kitGroup = KitGroup(
            id = lowercaseID,
        )

        with(KitGroupService.cached()) {
            KitGroupService.cached().groups += kitGroup
            KitGroupService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You created a new kit group with the ID ${CC.YELLOW}$lowercaseID${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("delete")
    @CommandCompletion("@kit-groups")
    @Description("Delete an existing kit group.")
    fun onDelete(player: ScalaPlayer, kitGroup: KitGroup)
    {
        // handle default kit group deletion in the command
        // as well as container so we can prompt users to not
        // do this!
        if (kitGroup.id == KitGroupContainer.DEFAULT)
        {
            throw ConditionFailedException(
                "You are unable to delete the default kit group!"
            )
        }

        with(KitGroupService.cached()) {
            KitGroupService.cached().remove(kitGroup)
            KitGroupService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You deleted the existing kit group ${CC.YELLOW}${kitGroup.id}${CC.GREEN}."
        )
    }
}
