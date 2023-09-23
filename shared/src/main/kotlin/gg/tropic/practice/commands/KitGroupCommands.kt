package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupContainer
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent

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
        player.sendMessage(
            "${CC.GREEN}Viewing kit group ${CC.WHITE}${kitGroup.id}${CC.GREEN}:",
            "${CC.GRAY}Associated with kits ${CC.WHITE}(${
                kits.size
            })${CC.GRAY}:",
            "${CC.WHITE}[click a kit to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in kits.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.id}${CC.WHITE}${
                        if (i != (kits.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view kit information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kit info ${group.id}"
                )

            listFancyMessage.components.addAll(
                uniqueInfoComponent.components
            )
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }

    @AssignPermission
    @Subcommand("list")
    @Description("List all kit groups.")
    fun onList(player: ScalaPlayer)
    {
        val kitGroups = KitGroupService.cached().groups

        player.sendMessage(
            "${CC.GREEN}All kit groups:",
            "${CC.WHITE}[click a kit to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in kitGroups.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.id}${CC.WHITE}${
                        if (i != (kitGroups.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view kit group information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kitgroup info ${group.id}"
                )

            listFancyMessage.components.addAll(
                uniqueInfoComponent.components
            )
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
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

        val kitGroup = KitGroup(id = lowercaseID)

        with(KitGroupService.cached()) {
            KitGroupService.cached().add(kitGroup)
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
            "${CC.GREEN}You deleted the kit group ${CC.YELLOW}${kitGroup.id}${CC.GREEN}."
        )
    }
}
