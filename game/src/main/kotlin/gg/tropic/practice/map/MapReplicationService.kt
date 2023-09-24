package gg.tropic.practice.map

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.replications.Replication
import gg.tropic.practice.replications.ReplicationManagerService
import gg.tropic.practice.replications.ReplicationStatus
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
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

        ReplicationManagerService.buildNewReplication = { map, expectation ->
            generateArenaWorld(map)
                .thenAccept { repl ->
                    repl.scheduledForExpectation = expectation
                    mapReplications += repl
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
                        SlimePropertyMap() /* TODO: arena.properties Do we need this? */
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
        val worldName =
            "${arena.name}-${
                UUID.randomUUID().toString().substring(0..7)
            }"

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
                return@thenApply BuiltMapReplication(arena, it)
            }
    }
}
