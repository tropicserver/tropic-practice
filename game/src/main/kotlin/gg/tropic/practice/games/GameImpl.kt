package gg.tropic.practice.games

import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.expectation.DuelExpectation
import gg.tropic.practice.expectation.ExpectationService
import gg.tropic.practice.feature.GameReportFeature
import gg.tropic.practice.games.loadout.CustomLoadout
import gg.tropic.practice.games.loadout.DefaultLoadout
import gg.tropic.practice.games.loadout.SelectedLoadout
import gg.tropic.practice.games.tasks.GameStartTask
import gg.tropic.practice.games.tasks.GameStopTask
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.map.MapReplicationService
import gg.tropic.practice.map.MapService
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.profile.loadout.Loadout
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.ItemUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.apache.commons.lang3.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
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
    kit: Kit,
    var state: GameState,
    private val mapId: String
) : AbstractGame(expectation, teams, kit), CompositeTerminable by CompositeTerminable.create()
{
    @Transient
    var activeCountdown = 5

    private val snapshots =
        mutableMapOf<UUID, GameReportSnapshot>()

    val map: gg.tropic.practice.map.Map
        get() = MapService.mapWithID(mapId)!!

    var arenaWorldName: String? = null

    val arenaWorld: World
        get() = Bukkit.getWorld(this.arenaWorldName)

    private val audiences: BukkitAudiences
        get() = ExpectationService.audiences

    private fun durationMillis() =
        System.currentTimeMillis() - this.startTimestamp

    private val selectedKitLoadouts = mutableMapOf<UUID, SelectedLoadout>()

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
                map = this.mapId,
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
                map = this.mapId,
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
        stopTask.task.bindWith(this)
    }

    fun initializeAndStart()
    {
        val startTask = GameStartTask(this)
        startTask.task = Schedulers.sync()
            .runRepeating(
                startTask,
                0L, TimeUnit.SECONDS,
                1L, TimeUnit.SECONDS
            )
        startTask.task.bindWith(this)

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

    fun closeAndCleanup(
        reason: String,
        kickPlayers: Boolean = true
    )
    {
        if (isClosed)
        {
            return
        }

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

                val online = Players.all()
                    .filter {
                        it.location.world.name == arenaWorld.name
                    }
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
                        MapReplicationService.removeReplicationMatchingWorld(arenaWorld)
                        Bukkit.unloadWorld(
                            arenaWorld, false
                        )
                    }
                }
        }.onFailure {
            it.printStackTrace()
        }

        closeAndReportException()
    }

    fun ensurePlaying() = this.state(GameState.Playing)

    fun state(state: GameState): Boolean
    {
        return this.state == state
    }

    fun flag(flag: FeatureFlag) = this.kit.features[flag] != null

    fun flagMetaData(flag: FeatureFlag, key: String): String?
    {
        if (!this.kit.features(flag))
        {
            return null
        }

        return this.kit
            .features[flag]
            ?.get(key)
            ?: flag.schema[key]
    }

    private val loadoutSelection = mutableMapOf<UUID, CompositeTerminable>()
    fun completeLoadoutSelection()
    {
        loadoutSelection.values.forEach(
            CompositeTerminable::closeAndReportException
        )
        loadoutSelection.clear()
    }

    private val defaultLoadout = DefaultLoadout(kit)
    private val defaultLoadoutID = UUID.randomUUID()

    fun enterLoadoutSelection(player: Player)
    {
        val profile = PracticeProfileService
            .find(player)
            ?: return run {
                println("applying default")
                defaultLoadout.apply(player)
            }

        val terminable = CompositeTerminable.create()
        terminable.with {
            if (!selectedKitLoadouts.containsKey(player.uniqueId))
            {
                selectedKitLoadouts[player.uniqueId] = defaultLoadout
            }
        }

        val applicableLoadouts = profile.customLoadouts
            .getOrDefault(kit.id, listOf())
            .map {
                @Suppress("USELESS_CAST")
                CustomLoadout(it, kit) as SelectedLoadout
            }
            .associateBy {
                UUID.randomUUID().toString()
            }
            .toMutableMap()
            .apply {
                this[defaultLoadoutID.toString()] = defaultLoadout
            }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter {
                it.action == Action.RIGHT_CLICK_AIR
            }
            .filter {
                ItemUtils.itemTagHasKey(it.item, "loadout")
            }
            .handler {
                val itemTagValue = ItemUtils.readItemTagKey(it.item, "loadout")
                val loadout = applicableLoadouts[itemTagValue]
                    ?: defaultLoadout

                selectedKitLoadouts[player.uniqueId] = loadout

                terminable.closeAndReportException()
                loadoutSelection.remove(player.uniqueId)

                loadout.apply(player)

                player.sendMessage(
                    "${CC.GREEN}You have selected the ${CC.WHITE}${loadout}${CC.GREEN} loadout!"
                )
            }
            .bindWith(terminable)

        applicableLoadouts.forEach { (t, u) ->
            val item = ItemBuilder
                .of(
                    if (t == defaultLoadoutID.toString())
                        Material.BOOK else Material.ENCHANTED_BOOK
                )
                .name("${CC.GREEN}${u.displayName()} ${CC.GRAY}(Right-Click)")
                .addToLore(
                    "${CC.GRAY}Click to select this loadout."
                )
                .build()

            player.inventory.addItem(
                ItemUtils.addToItemTag(item, "loadout", t)
            )
        }

        loadoutSelection[player.uniqueId] = terminable
    }
}
