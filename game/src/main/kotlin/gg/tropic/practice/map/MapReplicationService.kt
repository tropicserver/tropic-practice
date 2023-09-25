package gg.tropic.practice.map

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.api.world.properties.SlimeProperties
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.expectation.DuelExpectation
import gg.tropic.practice.expectation.ExpectationService
import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.replications.Replication
import gg.tropic.practice.replications.ReplicationManagerService
import gg.tropic.practice.replications.ReplicationStatus
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.util.CC
import org.bukkit.World
import org.bukkit.event.world.WorldLoadEvent
import java.util.*
import java.util.concurrent.CompletableFuture
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

    // TODO: New map changes don't propagate properly, might
    //  require restart for all game servers.
    private val readyMaps = mutableMapOf<String, ReadyMapTemplate>()
    private val mapReplications = mutableListOf<BuiltMapReplication>()

    @Configure
    fun configure()
    {
        slimePlugin = plugin.server.pluginManager
            .getPlugin("SlimeWorldManager") as SlimePlugin

        loader = slimePlugin.getLoader("mongodb")
        populateSlimeCache()

        preGenerateMapReplications().thenRun {
            plugin.logger.info(
                "Generated $TARGET_PRE_GEN_REPLICATIONS map replications for each of the ${MapService.maps().count()} available maps. This server currently has ${mapReplications.size} available replications."
            )
        }.exceptionally {
            plugin.logger.log(
                Level.SEVERE, "Failed to pre-generate map replications", it
            )
            return@exceptionally null
        }

        fun deleteExpectation(identifier: UUID)
        {
            DataStoreObjectControllerCache
                .findNotNull<DuelExpectation>()
                .delete(
                    identifier, DataStoreStorageType.REDIS
                )
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

        val buildGameResources = handler@{ expectationID: UUID ->
            val expectation = DataStoreObjectControllerCache
                .findNotNull<DuelExpectation>()
                .load(
                    expectationID,
                    DataStoreStorageType.REDIS
                )
                .join()
                ?: return@handler

            val kit = KitService.cached()
                .kits[expectation.kitId]
                ?: return@handler run {
                    deleteExpectation(expectation.identifier)
                }

            val newGame = GameImpl(
                expectation = expectation.identifier,
                teams = expectation.teams,
                kit = kit,
                state = GameState.Waiting,
                mapId = expectation.mapId
            )

            val scheduledMap = findScheduledReplication(expectation.identifier)
                ?: return@handler run {
                    deleteExpectation(expectation.identifier)
                }

            scheduledMap.inUse = true
            newGame.arenaWorldName = scheduledMap.world.name

            val start = System.currentTimeMillis()

            Schedulers.async()
                .runRepeating(
                    { task ->
                        if (System.currentTimeMillis() >= start + 5000L)
                        {
                            newGame.closeAndCleanup(
                                "Opponents or teammates did not join on time!"
                            )
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
                    10L, 2L
                )

            DataStoreObjectControllerCache
                .findNotNull<GameImpl>()
                .save(newGame, DataStoreStorageType.REDIS)
                .join()

            GameService.games[expectation.identifier] = newGame
        }

        ReplicationManagerService.buildNewReplication = { map, expectation ->
            generateArenaWorld(map)
                .thenAccept { repl ->
                    repl.scheduledForExpectation = expectation
                    mapReplications += repl

                    buildGameResources(expectation)
                }
        }

        ReplicationManagerService.allocateExistingReplication = scope@{ map, expectation ->
            val replication = this.mapReplications
                .firstOrNull {
                    it.associatedMap.name == map.name && !it.inUse && it.scheduledForExpectation == null
                }
                ?: return@scope run {
                    println("No associated map found")
                    CompletableFuture.completedFuture(null)
                }

            replication.scheduledForExpectation = expectation
            buildGameResources(expectation)

            println("Scheduled shit")
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
    }

    fun findScheduledReplication(expectation: UUID) = mapReplications
        .firstOrNull { it.scheduledForExpectation == expectation }

    private const val TARGET_PRE_GEN_REPLICATIONS = 32
    private fun preGenerateMapReplications(): CompletableFuture<Void>
    {
        return CompletableFuture.allOf(
            *MapService.maps()
                .flatMap {
                    (0 until TARGET_PRE_GEN_REPLICATIONS)
                        .map { _ ->
                            generateArenaWorld(it).thenAccept { mapReplications += it }
                        }
                }
                .toTypedArray()
        )
    }

    private fun populateSlimeCache()
    {
        for (arena in MapService.maps())
        {
            kotlin.runCatching {
                val slimeWorld = slimePlugin
                    .loadWorld(
                        loader,
                        arena.associatedSlimeTemplate,
                        true,
                        SlimePropertyMap().apply {
                            setString(SlimeProperties.DIFFICULTY, "normal")
                            setBoolean(SlimeProperties.PVP, true)
                        }
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

    fun generateArenaWorld(arena: Map): CompletableFuture<BuiltMapReplication>
    {
        val worldName = UUID.randomUUID().toString()
        val readyMap = readyMaps[arena.name]
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("Map ${arena.name} does not have a ready SlimeWorld. Map changes have not propagated to this server?")
            )

        val future = CompletableFuture<World>()
        val terminable = CompositeTerminable.create()

        CompletableFuture.runAsync {
            // TODO: don't need to remember this shit hopefully
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

        Schedulers
            .async()
            .runLater({
                future.completeExceptionally(
                    IllegalStateException("The world did not load on time")
                )
            }, 20L * 5L)
            .bindWith(terminable)

        return future
            .thenCompose { world ->
                arena.metadata.clearSignLocations(world)
                    .thenApply { world }
            }
            .thenApply {
                it.setGameRuleValue("naturalRegeneration", "false")
                it.setGameRuleValue("sendCommandFeedback", "false")
                it.setGameRuleValue("logAdminCommands", "false")
                it.setGameRuleValue("pvp", "true")
                return@thenApply BuiltMapReplication(arena, it)
            }
    }
}
