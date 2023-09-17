package gg.tropic.practice.arena

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.api.world.SlimeWorld
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.arena.ArenaLocation
import gg.tropic.practice.ladder.DuelLadder
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Service
object ArenaService
{
    @Inject
    lateinit var plugin: PracticeGame

    private lateinit var slimePlugin: SlimePlugin
    private lateinit var fileLoader: SlimeLoader

    val arenas = mutableMapOf<String, Arena>()
    private val cached = mutableMapOf<String, SlimeWorld>()

    @Configure
    fun configure()
    {
        slimePlugin = plugin.server.pluginManager
            .getPlugin("SlimeWorldManager") as SlimePlugin

        fileLoader = slimePlugin.getLoader("file")

        populateArenas()
        populateSlimeCache()
    }

    private fun populateSlimeCache()
    {
        for (arena in arenas.values)
        {
            kotlin.runCatching {
                val slimeWorld = slimePlugin
                    .loadWorld(
                        fileLoader, arena.arenaFile,
                        true, arena.properties
                    )

                cached[arena.uniqueId] = slimeWorld
                plugin.logger.info(
                    "Populated slime cache with SlimeWorld for arena ${arena.uniqueId}."
                )
            }.onFailure {
                plugin.logger.log(
                    Level.SEVERE, "Failed to populate cache", it
                )
            }
        }
    }

    fun generateArenaWorld(arena: Arena): CompletableFuture<World>
    {
        val worldName =
            "${arena.uniqueId}-${
                UUID.randomUUID().toString().substring(0..5)
            }"

        slimePlugin
            .generateWorld(
                cached[arena.uniqueId]!!.clone(worldName)
            )

        return CompletableFuture
            .supplyAsync {
                var world = Bukkit.getWorld(worldName)

                while (world == null)
                {
                    world = Bukkit.getWorld(worldName)
                }

                return@supplyAsync world
            }
            .thenApply {
                it.setGameRuleValue("naturalRegeneration", "false")
                it.setGameRuleValue("sendCommandFeedback", "false")
                it.setGameRuleValue("logAdminCommands", "false")

                return@thenApply it
            }
    }

    fun selectRandomCompatible(
        duelLadder: DuelLadder
    ): Arena?
    {
        return arenas.values
            .shuffled()
            .firstOrNull {
                it.compatible.contains(duelLadder)
            }
    }

    private fun populateArenas()
    {
        arenas["Snowy"] = Arena(
            uniqueId = "Snowy",
            display = "Snowy",
            arenaFile = "Arenas",
            compatible = listOf(
                DuelLadder.Bow, DuelLadder.UHC
            ),
            spawns = mapOf(
                0 to ArenaLocation(1085.5, 20.0, -68.5, 90.0F, 0.0F),
                1 to ArenaLocation(1023.5, 20.0, -68.5, -90.0F, 0.0F)
            )
        )

        arenas["Sumo"] = Arena(
            uniqueId = "Sumo",
            display = "Sumo",
            arenaFile = "Sumo",
            compatible = listOf(DuelLadder.Sumo),
            spawns = mapOf(
                0 to ArenaLocation(8.5, 68.0, -2.5, -180.0F, 0.0F),
                1 to ArenaLocation(8.5, 68.0, -12.5, 0.0F, 0.0F)
            )
        )

        arenas["Desert"] = Arena(
            uniqueId = "Desert",
            display = "Desert",
            arenaFile = "Arenas",
            compatible = listOf(DuelLadder.Bow, DuelLadder.UHC),
            spawns = mapOf(
                0 to ArenaLocation(628.5, 150.0, 44.5, 180F, 0F),
                1 to ArenaLocation(628.5, 150.0, -14.5, 0F, 0F)
            )
        )

        arenas["Jungle"] = Arena(
            uniqueId = "Jungle",
            display = "Jungle",
            arenaFile = "Arenas",
            compatible = listOf(DuelLadder.Bow, DuelLadder.UHC),
            spawns = mapOf(
                0 to ArenaLocation(63.5, 37.0, -123.5, 0F, 0F),
                1 to ArenaLocation(63.5, 37.0, -64.5, 180F, 0F)
            )
        )
    }
}
