package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
@AutoRegister
@CommandAlias("map")
@CommandPermission("practice.command.map")
object MapCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("groups add")
    @CommandCompletion("@stranger-kit-groups")
    @Description("Associate a kit group with a map.")
    fun onGroupsAdd(player: ScalaPlayer, map: Map, group: KitGroup)
    {
        if (group.id in map.associatedKitGroups)
        {
            throw ConditionFailedException(
                "The kit group ${group.id} is already associated with map ${map.name}."
            )
        }

        map.associatedKitGroups += group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with map ${CC.YELLOW}${map.name}${CC.GREEN}."
        )
    }

    @Subcommand("groups remove")
    @CommandCompletion("@associated-kit-groups")
    @Description("Remove an associated kit group from a map.")
    fun onGroupsRemove(player: ScalaPlayer, map: Map, group: KitGroup)
    {
        if (group.id !in map.associatedKitGroups)
        {
            throw ConditionFailedException(
                "The kit group ${group.id} is not associated with map ${map.name}."
            )
        }

        map.associatedKitGroups -= group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.RED}Kit group ${CC.YELLOW}${group.id}${CC.RED} is no longer associated with map ${CC.YELLOW}${map.name}${CC.RED}."
        )
    }

    @Subcommand("groups list")
    @Description("List all associated kit groups for a map.")
    fun onGroupsList(player: ScalaPlayer, map: Map)
    {
        player.sendMessage(
            "${CC.GREEN}Associated kit groups for map ${map.name}:",
            "${CC.GRAY}${
                map.associatedKitGroups.joinToString(", ")
            }"
        )
    }
}