package gg.tropic.practice.games

import com.cryptomorin.xseries.XSound
import gg.tropic.practice.arena.Arena
import gg.tropic.practice.arena.ArenaService
import gg.tropic.practice.expectation.ExpectationService
import gg.tropic.practice.feature.GameReportFeature
import gg.tropic.practice.games.tasks.GameStartTask
import gg.tropic.practice.games.tasks.GameStopTask
import gg.tropic.practice.expectation.DuelExpectation
import gg.tropic.practice.games.AbstractGame
import gg.tropic.practice.games.GameReport
import gg.tropic.practice.games.GameReportSnapshot
import gg.tropic.practice.games.GameReportStatus
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.ladder.DuelLadder
import gg.tropic.practice.ladder.DuelLadderFlag
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.storable.IDataStoreObject
import gg.scala.store.storage.type.DataStoreStorageType
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.player.PlayerSnapshot
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.minecraft.server.v1_8_R3.ChunkRegionLoader
import org.apache.commons.lang3.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
class GameImpl(
    expectation: UUID,
    teams: Map<GameTeamSide, GameTeam>,
    ladder: DuelLadder,
    var state: GameState,
    private val arenaName: String
) : IDataStoreObject, AbstractGame(expectation, teams, ladder)
{
    @Transient
    var activeCountdown = 5

    private val snapshots =
        mutableMapOf<UUID, GameReportSnapshot>()

    val arena: Arena
        get() = ArenaService.arenas[this.arenaName]!!

    var arenaWorldName: String? = null

    val arenaWorld: World
        get() = Bukkit.getWorld(this.arenaWorldName)

    private val audiences: BukkitAudiences
        get() = ExpectationService.audiences

    private fun durationMillis() =
        System.currentTimeMillis() - this.startTimestamp

    fun takeSnapshot(player: Player)
    {
        this.snapshots[player.uniqueId] =
            GameReportSnapshot(player)
    }

    fun takeSnapshotIfNotAlreadyExists(player: Player)
    {
        if (this.snapshots.containsKey(player.uniqueId))
        {
            return
        }

        this.snapshots[player.uniqueId] =
            GameReportSnapshot(player)
    }

    fun complete(winner: GameTeam?)
    {
        this.toBukkitPlayers()
            .filterNotNull()
            .forEach {
                this.takeSnapshotIfNotAlreadyExists(it)
            }

        if (winner == null)
        {
            this.report = GameReport(
                identifier = UUID.randomUUID(),
                winners = listOf(), losers = listOf(),
                snapshots = snapshots,
                duration = this.durationMillis(),
                arena = this.arenaName,
                status = GameReportStatus.ForcefullyClosed
            )
        } else
        {
            val opponent = this.getOpponent(winner)
                ?: return

            this.report = GameReport(
                identifier = UUID.randomUUID(),
                winners = winner.players,
                losers = opponent.players,
                snapshots = snapshots,
                duration = this.durationMillis(),
                arena = this.arenaName,
                status = GameReportStatus.Completed
            )
        }

        this.state = GameState.Completed
        GameReportFeature.saveSnapshotForAllParticipants(report!!)

        val stopTask =
            GameStopTask(
                this, this.report!!
            )

        this.activeCountdown = 5

        stopTask.task = Schedulers.sync()
            .runRepeating(
                stopTask,
                0L, TimeUnit.SECONDS,
                1L, TimeUnit.SECONDS
            )
    }

    fun initializeAndStart()
    {
        val startTask = GameStartTask(this)
        startTask.task = Schedulers.sync()
            .runRepeating(
                startTask,
                1L, TimeUnit.SECONDS,
                1L, TimeUnit.SECONDS
            )

        DataStoreObjectControllerCache
            .findNotNull<DuelExpectation>()
            .delete(
                this.expectation,
                DataStoreStorageType.REDIS
            )
    }

    fun audiencesIndexed(lambda: (Audience, UUID) -> Unit) =
        this.toBukkitPlayers()
            .filterNotNull()
            .forEach {
                lambda(this.audiences.player(it), it.uniqueId)
            }

    fun audiences(lambda: (Audience) -> Unit) =
        this.toBukkitPlayers()
            .filterNotNull()
            .forEach {
                this.audiences.player(it).apply(lambda)
            }

    fun sendMessage(vararg message: String)
    {
        for (line in message)
        {
            this.sendMessage(line)
        }
    }

    fun playSound(sound: Sound)
    {
        this.toBukkitPlayers()
            .filterNotNull()
            .forEach {
                it.playSound(it.location, sound, 1.0f, 1.0f)
            }
    }

    fun sendMessage(message: String)
    {
        this.toBukkitPlayers()
            .filterNotNull()
            .forEach {
                it.sendMessage(message)
            }
    }

    fun getTeamOf(player: Player) = this.teams.values
        .first {
            player.uniqueId in it.players
        }

    fun getOpponent(player: Player) =
        this.getOpponent(
            this.getTeamOf(player)
        )

    fun getOpponent(team: GameTeam) = this.teams[
            when (team.side)
            {
                GameTeamSide.A -> GameTeamSide.B
                GameTeamSide.B -> GameTeamSide.A
            }
    ]

    fun getDuration(): String
    {
        if (
            !this.ensurePlaying() &&
            !this.state(GameState.Completed)
        )
        {
            // This will return a capitalized name state which
            // makes sense in the context this function will be used in.
            return this.state.name
        }

        return DurationFormatUtils.formatDuration(
            System.currentTimeMillis() - this.startTimestamp, "mm:ss"
        )
    }

    fun toBukkitPlayers() = this.teams.values
        .flatMap {
            it.toBukkitPlayers()
        }

    fun toPlayers() = this.teams.values
        .flatMap {
            it.players
        }

    private var closed = false

    fun closeAndCleanup(
        reason: String,
        kickPlayers: Boolean = true
    )
    {
        if (closed)
        {
            return
        }

        this.closed = true

        kotlin.runCatching {
            Logger.getGlobal().info(
                "[Duels] cleaning up $expectation for $reason."
            )

            GameService.games
                .remove(this.expectation)

            if (kickPlayers)
            {
                this.sendMessage(
                    "${CC.RED}The game has ended:",
                    "${CC.WHITE}$reason"
                )

                val online = this.toBukkitPlayers()
                    .filterNotNull()
                    .toTypedArray()

                if (online.isNotEmpty())
                {
                    GameService.redirector.redirect(*online)
                }
            }

            DataStoreObjectControllerCache
                .findNotNull<GameImpl>()
                .delete(
                    this.identifier,
                    DataStoreStorageType.REDIS
                )
                .thenRun {
                    Tasks.delayed(20L) {
                        Bukkit.unloadWorld(
                            this.arenaWorld, false
                        )
                    }
                }
                .thenComposeAsync {
                    DataStoreObjectControllerCache
                        .findNotNull<DuelExpectation>()
                        .delete(
                            this.identifier,
                            DataStoreStorageType.REDIS
                        )
                }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun ensurePlaying() = this.state(GameState.Playing)

    fun state(state: GameState): Boolean
    {
        return this.state == state
    }

    fun flag(flag: DuelLadderFlag) = flag in this.ladder.flags
}
