package gg.tropic.practice.expectation

import gg.tropic.practice.PracticeConfig
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.resetAttributes
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.map.MapReplicationService
import gg.tropic.practice.map.MapService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.World
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Service
object ExpectationService
{
    private val expected = mutableMapOf<UUID, DuelExpectation>()

    @Inject
    lateinit var plugin: PracticeGame

    @Inject
    lateinit var redirector: ServerAggregateHandler

    @Inject
    lateinit var audiences: BukkitAudiences

    @Configure
    fun configure()
    {
        DataStoreObjectControllerCache.create<DuelExpectation>()

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
                expected.remove(game.expectation)
                return true
            }

            return false
        }

        val waitingLocation = plugin
            .config<PracticeConfig>()
            .waitingLocation

        Events
            .subscribe(
                AsyncPlayerPreLoginEvent::class.java,
                EventPriority.HIGHEST
            )
            .handler { event ->
                val expectation = DataStoreObjectControllerCache
                    .findNotNull<DuelExpectation>()
                    .loadAll(DataStoreStorageType.REDIS)
                    .join().values
                    .find {
                        event.uniqueId in it.players
                    }

                if (expectation == null)
                {
                    event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        """
                            ${CC.RED}We did not expect you to join this server!
                            ${CC.RED}Something's wrong? Contact an administrator for help.
                        """.trimIndent()
                    )
                    return@handler
                }

                expected[event.uniqueId] = expectation
            }
            .bindWith(plugin)

        Events
            .subscribe(
                PlayerJoinEvent::class.java,
                EventPriority.HIGHEST
            )
            .handler {
                val expectation = expected[it.player.uniqueId]
                    ?: return@handler kotlin.run {
                        it.player.sendMessage("${CC.RED}Woah! You're not supposed to be here.")
                        redirector.redirect(it.player)
                    }

                it.player.resetAttributes()
                it.player.removeMetadata("spectator", plugin)

                val game = GameService.games
                    .values.find { game ->
                        game.expectation == expectation.identifier
                    }

                fun GameImpl.teleportToSpawn()
                {
                    it.player.teleport(
                        map
                            .findSpawnLocationMatchingTeam(
                                getTeamOf(it.player).side
                            )!!
                            .toLocation(arenaWorld)
                    )
                }

                if (game == null)
                {
                    val kit = KitService.cached()
                        .kits[expectation.kitId]
                        ?: return@handler run {
                            it.player.kickPlayer(
                                "${CC.RED}We couldn't find the kit you were supposed to play with!"
                            )

                            deleteExpectation(expectation.identifier)
                        }

                    val newGame = GameImpl(
                        expectation = expectation.identifier,
                        teams = expectation.teams,
                        kit = kit,
                        state = GameState.Waiting,
                        mapId = expectation.mapId
                    )

                    val scheduledMap = MapReplicationService
                        .findScheduledReplication(expectation.identifier)
                        ?: return@handler run {
                            it.player.kickPlayer(
                                "${CC.RED}You don't have a map allocated for this game!"
                            )

                            deleteExpectation(expectation.identifier)
                        }

                    scheduledMap.inUse = true
                    newGame.arenaWorldName = scheduledMap.world.name

                    if (!startIfReady(newGame))
                    {
                        Schedulers.sync()
                            .callLater(
                                {
                                    if (newGame.state == GameState.Waiting)
                                    {
                                        newGame.closeAndCleanup(
                                            "Opponents or teammates did not join on time!"
                                        )
                                    }
                                },
                                10L, TimeUnit.SECONDS
                            )
                    }

                    DataStoreObjectControllerCache
                        .findNotNull<GameImpl>()
                        .save(newGame, DataStoreStorageType.REDIS)

                    GameService.games[expectation.identifier] = newGame
                    newGame.teleportToSpawn()
                } else
                {
                    game.teleportToSpawn()
                    if (game.state(GameState.Waiting))
                    {
                        startIfReady(game)
                    }
                }
            }
            .bindWith(plugin)
    }
}
