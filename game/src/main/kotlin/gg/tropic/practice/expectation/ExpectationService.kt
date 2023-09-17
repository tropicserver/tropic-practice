package gg.tropic.practice.expectation

import gg.tropic.practice.PracticeConfig
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.arena.ArenaService
import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.resetAttributes
import gg.tropic.practice.games.GameReport
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.platform.bukkit.BukkitAudiences
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
        DataStoreObjectControllerCache.create<GameReport>()

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

                if (game == null)
                {
                    val compatible = ArenaService
                        .selectRandomCompatible(
                            expectation.kitId
                        )
                    if (compatible == null)
                    {
                        it.player.kickPlayer(
                            "${CC.RED}We couldn't find a compatible arena for you to play in!"
                        )

                        deleteExpectation(expectation.identifier)
                        return@handler
                    }

                    val newGame = GameImpl(
                        expectation = expectation.identifier,
                        teams = expectation.teams,
                        kit = expectation.kitId,
                        state = GameState.Generating,
                        arenaName = compatible.uniqueId
                    )

                    // NEVER join this CompletableFuture
                    ArenaService
                        .generateArenaWorld(compatible)
                        .thenAccept { world ->
                            Tasks.sync {
                                newGame.arenaWorldName = world.name

                                if (!startIfReady(newGame))
                                {
                                    newGame.state = GameState.Waiting

                                    Schedulers.sync()
                                        .callLater(
                                            {
                                                val offline = newGame
                                                    .toBukkitPlayers()
                                                    .any { other ->
                                                        other == null
                                                    }

                                                if (offline)
                                                {
                                                    newGame.closeAndCleanup(
                                                        "Opponents or teammates did not join on time!"
                                                    )
                                                }
                                            },
                                            10L, TimeUnit.SECONDS
                                        )
                                }
                            }
                        }

                    DataStoreObjectControllerCache
                        .findNotNull<GameImpl>()
                        .save(newGame, DataStoreStorageType.REDIS)

                    GameService.games[expectation.identifier] = newGame

                    Tasks.sync {
                        it.player.teleport(waitingLocation)
                    }

                    it.player.sendMessage(
                        "${CC.GREEN}Generating the arena..."
                    )
                } else
                {
                    Tasks.sync {
                        it.player.teleport(waitingLocation)
                    }

                    if (game.state == GameState.Generating)
                    {
                        it.player.sendMessage(
                            "${CC.GREEN}Generating the arena..."
                        )
                        return@handler
                    }

                    if (game.state(GameState.Waiting))
                    {
                        startIfReady(game)
                    }
                }
            }
            .bindWith(plugin)
    }
}
