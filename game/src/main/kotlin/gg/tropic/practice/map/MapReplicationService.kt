package gg.tropic.practice.map

import com.comphenix.packetwrapper.WrapperPlayServerLogin
import com.comphenix.packetwrapper.WrapperPlayServerWorldEvent
import com.comphenix.protocol.PacketType
import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.api.world.properties.SlimeProperties
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import com.grinderwolf.swm.plugin.config.WorldData
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
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
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.protocol.Protocol
import me.lucko.helper.terminable.composite.CompositeTerminable
import org.bukkit.World
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import org.bukkit.event.world.WorldLoadEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
@Service
object MapReplicationService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private lateinit var slimePlugin: SlimePlugin
    private lateinit var loader: SlimeLoader

    private val readyMaps = mutableMapOf<String, ReadyMapTemplate>()
    private val mapReplications = CopyOnWriteArrayList<BuiltMapReplication>()

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

        preGenerateMapReplications().thenRun {
            plugin.logger.info(
                "Generated $TARGET_PRE_GEN_REPLICATIONS map replications for each of the ${
                    MapService.maps().count()
                } available maps. This server currently has ${mapReplications.size} available replications."
            )

            plugin.flavor().inject(ReplicationAutoScaleTask)
        }.exceptionally {
            plugin.logger.log(
                Level.SEVERE, "Failed to pre-generate map replications", it
            )
            return@exceptionally null
        }

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

            GameService.games[expectation.identifier] = newGame
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

    private fun generateArenaWorld(arena: Map): CompletableFuture<BuiltMapReplication>
    {
        val worldName = "${arena.name}-${
            UUID.randomUUID().toString().substring(0..5)
        }"
        val readyMap = readyMaps[arena.name]
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("Map ${arena.name} does not have a ready SlimeWorld. Map changes have not propagated to this server?")
            )

        val future = CompletableFuture<World>()
        val terminable = CompositeTerminable.create()

        CompletableFuture
            .runAsync {
                slimePlugin.generateWorld(
                    readyMap.slimeWorld.clone(worldName)
                )
            }

        Events
            .subscribe(WorldLoadEvent::class.java)
            .filter {
                it.world.name == worldName
            }
            .handler {
                future.complete(it.world)
                terminable.closeAndReportException()
            }
            .bindWith(terminable)

        return future
            .thenCompose { world ->
                arena.metadata.clearSignLocations(world)
            }
            .thenApply {
                BuiltMapReplication(arena, it)
            }
    }
}
