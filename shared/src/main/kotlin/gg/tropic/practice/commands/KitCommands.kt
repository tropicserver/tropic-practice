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
        player.sendMessage(" ")
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
        player.sendMessage("${CC.GRAY}Armor Contents ${CC.WHITE}(${kit.armorContents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
        player.sendMessage("${CC.GRAY}Inventory Contents ${CC.WHITE}(${kit.contents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
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
