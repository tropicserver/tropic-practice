package gg.tropic.practice.command

import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapManageServices
import gg.tropic.practice.map.MapService
import gg.tropic.practice.map.utilities.MapMetadataScanUtilities
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import org.bukkit.Bukkit
import org.bukkit.Sound
import java.util.*

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
@AutoRegister
@CommandAlias("mapmanage")
@CommandPermission("op")
object MapManageCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("create")
    @Description("Creates a new map based off of a Slime world template.")
    fun onCreate(player: ScalaPlayer, slimeTemplate: String, mapName: String)
    {
        if (MapService.mapWithID(mapName) != null)
        {
            throw ConditionFailedException(
                "A map with the ID $mapName already exists."
            )
        }

        val world = runCatching {
            MapManageServices.slimePlugin.loadWorld(
                MapManageServices.loader,
                slimeTemplate,
                true,
                SlimePropertyMap()
            )
        }.getOrNull() ?: throw ConditionFailedException(
            "The world $slimeTemplate either does not exist or is a locked world due to write being enabled."
        )

        MapManageServices.slimePlugin.generateWorld(world)

        with(player.bukkit()) {
            val currentLocation = location
            allowFlight = true
            isFlying = true

            val devToolsMap = Bukkit.getWorld(slimeTemplate)
            teleport(devToolsMap.spawnLocation)

            InputPrompt()
                .withText("${CC.GREEN}Move to the lowest corner of the map and type something in chat.")
                .acceptInput { _, _ ->
                    val lowest = location
                    playSound(location, Sound.NOTE_PLING, 1.0f, 1.0f)

                    Tasks.delayed(1L) {
                        InputPrompt()
                            .withText("${CC.GREEN}Move to the highest corner of the map and type something in chat.")
                            .acceptInput { _, _ ->
                                val highest = location

                                sendMessage("${CC.GRAY}Building a metadata copy...")
                                val bounds = Cuboid(l1 = lowest, l2 = highest)
                                val metadata = MapMetadataScanUtilities
                                    .buildMetadataFor(bounds)

                                sendMessage("${CC.B_GRAY}(!)${CC.GRAY} Created a metadata copy! We're now going to build the map data model...")

                                val map = Map(
                                    identifier = UUID.randomUUID(),
                                    name = mapName,
                                    bounds = bounds,
                                    metadata = metadata,
                                    displayName = mapName,
                                    associatedSlimeTemplate = slimeTemplate
                                )

                                with(MapService.cached()) {
                                    maps[map.identifier] = map
                                    MapService.sync(this)

                                    playSound(location, Sound.FIREWORK_LAUNCH, 1.0f, 1.0f)
                                    sendMessage("${CC.B_GREEN}(!)${CC.GREEN} Successfully created map ${CC.YELLOW}${map.name}${CC.GREEN}!")
                                }

                                teleport(currentLocation)
                                Bukkit.unloadWorld(devToolsMap, false)

                                isFlying = false
                                allowFlight = false

                                sendMessage("${CC.B_GOLD}(!) ${CC.GOLD}Unloaded the DTT world, you're all set! Please note that map changes (adding/deleting maps) won't propagate to the game servers immediately. A restart is required for the changes to take effect.")
                            }
                            .start(this)
                    }
                }
                .start(this)
        }
    }
}
