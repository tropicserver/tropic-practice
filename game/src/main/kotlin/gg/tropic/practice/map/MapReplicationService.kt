package gg.tropic.practice.map

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.plugin.config.WorldData
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Close
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.autoscale.ReplicationAutoScaleTask
import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.services.ReplicationManagerService
import gg.tropic.practice.replications.models.Replication
import gg.tropic.practice.replications.models.ReplicationStatus
import me.lucko.helper.Schedulers
import net.minecraft.server.v1_8_R3.MinecraftServer
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level
import kotlin.concurrent.thread

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
@Service(priority = 20)
object MapReplicationService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private lateinit var slimePlugin: SlimePlugin
    private lateinit var loader: SlimeLoader

    private val readyMaps = mutableMapOf<String, ReadyMapTemplate>()
    internal val mapReplications = CopyOnWriteArrayList<BuiltMapReplication>()

    fun removeReplicationMatchingWorld(world: World) = mapReplications
        .removeIf {
            it.world.name == world.name
        }

    @Configure
    fun configure()
    {
        slimePlugin = plugin.server.pluginManager
            .getPlugin("SlimeWorldManager") as SlimePlugin

        loader = slimePlugin.getLoader("mongodb")
        populateSlimeCache()

        fun startIfReady(game: GameImpl): Boolean
        {
            if (
                game.toBukkitPlayers()
                    .none { other ->
                        other == null
                    }
            )
            {
                game.initializeAndStart()
                return true
            }

            return false
        }

        val buildGameResources = handler@{ expectation: GameExpectation ->
            val kit = KitService.cached()
                .kits[expectation.kitId]
                ?: return@handler

            val newGame = GameImpl(
                expectation = expectation,
                kit = kit
            )
            newGame.buildResources()

            val scheduledMap = findScheduledReplication(expectation.identifier)
                ?: return@handler

            scheduledMap.inUse = true
            newGame.arenaWorldName = scheduledMap.world.name

            val start = System.currentTimeMillis()

            Schedulers
                .async()
                .runRepeating(
                    { task ->
                        if (System.currentTimeMillis() >= start + 5000L)
                        {
                            newGame.state = GameState.Completed
                            newGame.closeAndCleanup()

                            task.closeAndReportException()
                            return@runRepeating
                        }

                        if (newGame.state == GameState.Waiting)
                        {
                            if (startIfReady(newGame))
                            {
                                task.closeAndReportException()
                            }
                        }
                    },
                    0L, 1L
                )

            GameService.gameMappings[expectation.identifier] = newGame
        }

        ReplicationManagerService.buildNewReplication = { map, expectation ->
            generateArenaWorld(map)
                .thenAccept { repl ->
                    repl.scheduledForExpectedGame = expectation.identifier
                    mapReplications += repl

                    buildGameResources(expectation)
                }
        }

        ReplicationManagerService.allocateExistingReplication = scope@{ map, expectation ->
            val replication = this.mapReplications
                .firstOrNull {
                    it.associatedMap.name == map.name && !it.inUse
                        && it.scheduledForExpectedGame == null
                }
                ?: return@scope run {
                    CompletableFuture.completedFuture(null)
                }

            replication.scheduledForExpectedGame = expectation.identifier
            buildGameResources(expectation)

            return@scope CompletableFuture.completedFuture(null)
        }

        ReplicationManagerService.bindToStatusService {
            val replicationStatuses = mapReplications
                .map {
                    Replication(
                        associatedMapName = it.associatedMap.name,
                        name = it.world.name, inUse = it.inUse,
                        server = ServerSync.local.id
                    )
                }
                .groupBy {
                    it.associatedMapName
                }

            ReplicationStatus(replicationStatuses)
        }

        MapService.onPostReload = ::populateSlimeCache
        startWorldRequestThread()

        ReplicationAutoScaleTask.start()
    }

    @Close
    fun close()
    {
        ReplicationAutoScaleTask.interrupt()
    }

    fun findScheduledReplication(expectation: UUID) = mapReplications
        .firstOrNull { it.scheduledForExpectedGame == expectation }

    fun findAllAvailableReplications(map: Map) = mapReplications
        .filter {
            !it.inUse && it.associatedMap.name == map.name
        }

    fun generateMapReplications(
        mappings: kotlin.collections.Map<Map, Int>
    ): CompletableFuture<Void>
    {
        return CompletableFuture.allOf(
            *mappings.entries
                .flatMap {
                    (0 until it.value)
                        .map { _ ->
                            generateArenaWorld(it.key)
                                .thenAccept { replication ->
                                    mapReplications += replication
                                }
                        }
                }
                .toTypedArray()
        )
    }

    private const val TARGET_PRE_GEN_REPLICATIONS = 16
    private fun preGenerateMapReplications(): CompletableFuture<Void>
    {
        return generateMapReplications(
            MapService.maps().associateWith { TARGET_PRE_GEN_REPLICATIONS }
        )
    }

    private fun populateSlimeCache()
    {
        for (arena in MapService.maps())
        {
            kotlin.runCatching {
                val worldData = WorldData()
                worldData.isPvp = true
                worldData.difficulty = "normal"
                worldData.environment = "NORMAL"
                worldData.worldType = "DEFAULT"

                worldData.isAllowAnimals = false
                worldData.isAllowMonsters = false

                val slimeWorld = slimePlugin
                    .loadWorld(
                        loader,
                        arena.associatedSlimeTemplate,
                        true,
                        worldData.toPropertyMap()
                    )

                readyMaps[arena.name] = ReadyMapTemplate(slimeWorld)

                plugin.logger.info(
                    "Populated slime cache with SlimeWorld for arena ${arena.name}."
                )
            }.onFailure {
                plugin.logger.log(
                    Level.SEVERE, "Failed to populate cache", it
                )
            }
        }
    }

    fun startWorldRequestThread() = thread(isDaemon = true) {
        var lastRecordedTick = MinecraftServer.currentTick
        var lastTickSwitch = System.currentTimeMillis()
        var lastBroadcastTick = System.currentTimeMillis()

        while (true)
        {
            if (MinecraftServer.currentTick != lastRecordedTick)
            {
                if (System.currentTimeMillis() - lastTickSwitch > 5000L)
                {
                    plugin.logger.info("Pausing generation checks for 3s to let the server catch up...")
                    Thread.sleep(3000L)
                }

                lastRecordedTick = MinecraftServer.currentTick
                lastTickSwitch = System.currentTimeMillis()
            }

            if (System.currentTimeMillis() - lastTickSwitch > 5000L)
            {
                if (System.currentTimeMillis() - lastBroadcastTick > 5000L)
                {
                    lastBroadcastTick = System.currentTimeMillis()
                    plugin.logger.info("Server is halted at tick $lastRecordedTick... Waiting before expiring any pending generations")
                }
                continue
            }

            for (worldID in worldRequests.toMap().keys)
            {
                val bukkitWorld = Bukkit.getWorld(worldID.first)
                if (bukkitWorld == null)
                {
                    if (System.currentTimeMillis() - worldID.second >= 15_000L)
                    {
                        worldRequests.remove(worldID)
                            ?.completeExceptionally(
                                IllegalStateException(
                                    "Could not load world ${worldID.first}"
                                )
                            )
                        continue
                    }
                    continue
                }

                worldRequests.remove(worldID)?.complete(bukkitWorld)
            }

            Thread.sleep(350L)
        }
    }

    private val worldRequests = mutableMapOf<Pair<String, Long>, CompletableFuture<World>>()
    fun submitWorldRequest(worldID: String): CompletableFuture<World>
    {
        val future = CompletableFuture<World>()
        worldRequests[worldID to System.currentTimeMillis()] = future

        return future
    }

    private fun generateArenaWorld(arena: Map): CompletableFuture<BuiltMapReplication>
    {
        val worldName = "${arena.name}-${
            UUID.randomUUID().toString().substring(0..5)
        }"

        val readyMap = readyMaps[arena.name]
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("Map ${arena.name} does not have a ready SlimeWorld. Map changes have not propagated to this server?")
            )

        return CompletableFuture
            .runAsync {
                slimePlugin.generateWorld(
                    readyMap.slimeWorld.clone(worldName)
                )
            }
            .thenCompose {
                submitWorldRequest(worldName)
            }
            .thenApply {
                arena.metadata.clearSignLocations(it)
                BuiltMapReplication(arena, it)
            }
            .exceptionally {
                plugin.logger.log(
                    Level.SEVERE,
                    "Could not load/generate world $worldName",
                    (it as CompletionException).cause ?: it
                )
                return@exceptionally null
            }
    }
}
