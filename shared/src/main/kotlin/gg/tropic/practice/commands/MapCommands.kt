package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapService
import gg.tropic.practice.services.GameManagerService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import java.util.*
import java.util.concurrent.CompletableFuture

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

    @Subcommand("info")
    @Description("View information about a map")
    fun onInfo(player: ScalaPlayer, map: Map)
    {
        player.sendMessage("${CC.GREEN}Information for the map ${CC.B_WHITE}${map.displayName}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}ID: ${CC.WHITE}${map.name}")
        player.sendMessage("${CC.GRAY}Icon: ${CC.WHITE}${map.displayIcon.type.name.lowercase().replaceFirstChar { it.uppercase() }}")
        player.sendMessage("${CC.GRAY}Locked: ${if (map.locked) "${CC.GREEN}True" else "${CC.RED}False"}")
        player.sendMessage("${CC.GRAY}Associated Template: ${CC.WHITE}${map.associatedSlimeTemplate}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}Associated Kit Groups ${CC.WHITE}(${map.associatedKitGroups.size})${CC.GRAY}:")
        if (map.associatedKitGroups.isNotEmpty())
        {
            for (group in map.associatedKitGroups)
            {
                player.sendMessage(" &7▪ &f$group")
            }
        } else
        {
            player.sendMessage("&cNone Given")
        }
        player.sendMessage(" ")
    }

    @Subcommand("delete")
    @Description("Deletes a map based on the input given.")
    fun onDelete(player: ScalaPlayer, mapName: String): CompletableFuture<Void>
    {
        val map = MapService.mapWithID(mapName)
            ?: throw ConditionFailedException(
                "A map with the ID ${CC.YELLOW}$mapName ${CC.RED}does not exist."
            )

        return GameManagerService.allGames()
            .thenApply {
                it.filter { ref ->
                    ref.mapID == map.name
                }
            }
            .thenAccept {
                if (it.isNotEmpty())
                {
                    throw ConditionFailedException(
                        "You cannot delete this map as there are games ongoing bound to this map ID. Lock the map from being used in any new games by using ${CC.BOLD}/map lock ${map.name}${CC.RED}."
                    )
                }

                with(MapService.cached()) {
                    maps.remove(map.name)
                    MapService.sync(this)
                }

                player.sendMessage(
                    "${CC.GREEN}You deleted the map with the ID ${CC.YELLOW}${map.name}${CC.GREEN}."
                )
            }
    }

    @AssignPermission
    @Subcommand("lock|unlock")
    @Description("Lock a map from being used in any new games.")
    fun onLock(player: ScalaPlayer, map: Map)
    {
        with(MapService.cached()) {
            map.locked = !map.locked
            MapService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}The map lock is now: ${CC.YELLOW}${map.locked}"
        )
    }

    @Subcommand("list")
    @Description("List all maps.")
    fun onList(player: ScalaPlayer)
    {
        val maps = MapService.cached().maps.values
        player.sendMessage(
            "${CC.GREEN}All maps:",
            "${CC.WHITE}[click a map to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in maps.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.name}${CC.WHITE}${
                        if (i != (maps.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/map info ${group.name}"
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

    @Subcommand("groups add")
    @CommandCompletion("@maps @stranger-kit-groups")
    @Description("Associate a kit group with a map.")
    fun onGroupsAdd(player: ScalaPlayer, map: Map, group: KitGroup)
    {
        if (group.id in map.associatedKitGroups)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is already associated with map ${CC.YELLOW}${map.name}${CC.RED}."
            )
        }

        map.associatedKitGroups += group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with map ${CC.YELLOW}${map.name}${CC.GREEN}."
        )
    }

    @Subcommand("groups remove")
    @CommandCompletion("@maps @associated-kit-groups")
    @Description("Remove an associated kit group from a map.")
    fun onGroupsRemove(player: ScalaPlayer, map: Map, group: KitGroup)
    {
        if (group.id !in map.associatedKitGroups)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is not associated with map ${CC.YELLOW}${map.name}${CC.RED}."
            )
        }

        if (map.associatedKitGroups.size == 1)
        {
            throw ConditionFailedException(
                "You cannot remove this kit group when there are no other kit groups associated with this map. Please run ${CC.YELLOW}/map groups add ${map.name} __default__${CC.RED} to add back the default group before you remove the ${CC.YELLOW}${group.id}${CC.RED} group."
            )
        }

        map.associatedKitGroups -= group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.RED}Kit group ${CC.YELLOW}${group.id}${CC.RED} is no longer associated with map ${CC.YELLOW}${map.name}${CC.RED}."
        )
    }

    @Subcommand("groups list")
    @CommandCompletion("@maps")
    @Description("List all associated kit groups for a map.")
    fun onGroupsList(player: ScalaPlayer, map: Map)
    {
        player.sendMessage(
            "${CC.GREEN}All associated kit groups for map ${CC.B_WHITE}${map.name}${CC.GREEN}:",
            "${CC.WHITE}[click a kit group to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in map.associatedKitGroups.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}$group${CC.WHITE}${
                        if (i != (map.associatedKitGroups.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kitgroup info $group"
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
}
