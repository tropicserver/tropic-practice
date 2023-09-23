package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
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
    @Subcommand("info")
    @CommandCompletion("@kits")
    @Description("Show information about a given kit.")
    fun onInfo(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage("${CC.GREEN}Information for the kit ${CC.WHITE}${kit.displayName}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}ID: ${CC.WHITE}${kit.id}")
        player.sendMessage("${CC.GRAY}Enabled: ${if (kit.enabled) "${CC.GREEN}True" else "${CC.RED}False"}")
        player.sendMessage("${CC.GRAY}Icon: ${CC.WHITE}${
            kit.displayIcon.type.name.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else
                    it.toString()
            }
        }")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}Armor contents ${CC.WHITE}(${kit.armorContents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
        player.sendMessage("${CC.GRAY}Inventory contents ${CC.WHITE}(${kit.contents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
        //TODO: Implement a menu to view inventory contents once the kit management is implemented
        player.sendMessage(" ")
    }

    @AssignPermission
    @Subcommand("list")
    @Description("List all kits.")
    fun onList(player: ScalaPlayer)
    {
        val listFancyMessage = FancyMessage()
        val kits = KitService.cached().kits

        player.sendMessage(
            "${CC.GREEN}Kits"
        )

        for ((i, kit) in kits.values.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${kit.displayName}${
                        if (i != (kits.size - 1)) ", " else ""
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view kit information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kit info ${kit.id}"
                )

            listFancyMessage.components.add(uniqueInfoComponent.components[0])
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
    @Subcommand("groups add")
    @CommandCompletion("@kits @stranger-kit-groups-to-kit")
    @Description("Associate a kit with a kit group.")
    fun onGroupsAdd(player: ScalaPlayer, kit: Kit, group: KitGroup)
    {
        if (kit.id in group.contains)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is already associated with kit ${CC.YELLOW}${kit.displayName}${CC.RED}."
            )
        }

        group.contains += kit.id
        KitGroupService.sync(KitGroupService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @Subcommand("groups list")
    @CommandCompletion("@kits")
    @Description("List all associated kit groups with a kit.")
    fun onGroupsList(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage(
            "${CC.GREEN}All associated kit groups for kit ${CC.B_WHITE}${kit.displayName}${CC.GREEN}:",
            "${CC.WHITE}[click a kit group to view information]"
        )

        val listFancyMessage = FancyMessage()
        val associated = KitGroupService.groupsOf(kit)
        for ((i, group) in associated.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.id}${CC.WHITE}${
                        if (i != (associated.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
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
    @Subcommand("groups remove")
    @CommandCompletion("@kits @associated-kit-groups-with-kit")
    @Description("Remove an associated kit group from a kit.")
    fun onGroupsRemove(player: ScalaPlayer, kit: Kit, group: KitGroup)
    {
        if (kit.id !in group.contains)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is not associated with kit ${CC.YELLOW}${kit.displayName}${CC.RED}."
            )
        }

        val associated = KitGroupService.groupsOf(kit)
        if (associated.size == 1)
        {
            throw ConditionFailedException(
                "You cannot remove this kit group when there are no other kit groups associated with this kit. Please run ${CC.YELLOW}/kit groups add ${kit.id} __default__${CC.RED} to add back the default group before you remove the ${CC.YELLOW}${group.id}${CC.RED} group."
            )
        }

        group.contains -= kit.id
        KitGroupService.sync(KitGroupService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("create")
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

            with(KitGroupService) {
                with(cached()) {
                    default().contains += lowercaseID
                    KitGroupService.sync(this)
                }
            }

            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You created a new kit with the ID ${CC.YELLOW}$lowercaseID${CC.GREEN}."
        )
    }
}
