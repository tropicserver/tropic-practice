package gg.tropic.practice.player.hotbar

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import gg.scala.lemon.redirection.expectation.PlayerJoinWithExpectationEvent
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.parties.command.PartyCommand
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.commands.TournamentCommand
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.map.MapService
import gg.tropic.practice.menu.JoinQueueMenu
import gg.tropic.practice.menu.LeaderboardsMenu
import gg.tropic.practice.menu.PlayerMainMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.menu.pipeline.DuelRequestPipeline
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.region.Region
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.*

@Service
object LobbyHotbarService
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Inject
    lateinit var audiences: BukkitAudiences

    private val hotbarCache = mutableMapOf<PlayerState, HotbarPreset>()

    @Configure
    fun configure()
    {
        val idlePreset = HotbarPreset()

        val wasGameParticipant = mutableSetOf<UUID>()
        val loginTasks = mutableMapOf<UUID, MutableList<(Player) -> Unit>>()

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                wasGameParticipant.remove(it.player.uniqueId)
                loginTasks.remove(it.player.uniqueId)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .handler {
                if (it.player.uniqueId !in wasGameParticipant)
                {
                    with(PracticeConfigurationService.cached()) {
                        if (loginMOTD.isNotEmpty())
                        {
                            loginMOTD.forEach(it.player::sendMessage)
                        }
                    }
                }

                Tasks.delayed(5L) {
                    loginTasks[it.player.uniqueId]
                        ?.forEach { task ->
                            task.invoke(it.player)
                        }
                }
            }

        Events
            .subscribe(PlayerJoinWithExpectationEvent::class.java)
            .handler {
                if (it.response.parameters.containsKey("was-game-participant"))
                {
                    wasGameParticipant += it.uniqueId
                }

                if (it.response.parameters.containsKey("requeue-kit-id"))
                {
                    val rematchKitID = it.response.parameters["requeue-kit-id"]
                    val rematchQueueType = QueueType.valueOf(
                        it.response.parameters["requeue-queue-type"]!!
                    )

                    val kit = KitService.cached().kits[rematchKitID]
                        ?: return@handler

                    loginTasks.getOrPut(it.uniqueId, ::mutableListOf) += { player ->
                        QueueService.joinQueue(
                            kit = kit,
                            queueType = rematchQueueType,
                            teamSize = 1,
                            player = player
                        )
                    }
                }

                if (it.response.parameters.containsKey("rematch-kit-id"))
                {
                    val rematchKitID = it.response.parameters["rematch-kit-id"]
                    val rematchMapID = it.response.parameters["rematch-map-id"]
                    val rematchTargetID = it.response.parameters["rematch-target-id"]
                    val rematchTargetRegion = Region.valueOf(it.response.parameters["rematch-region"]!!)

                    val kit = KitService.cached().kits[rematchKitID]
                        ?: return@handler

                    val map = MapService.cached().maps[rematchMapID]
                        ?: return@handler

                    loginTasks.getOrPut(it.uniqueId, ::mutableListOf) += { player ->
                        val rematchItem = ItemBuilder
                            .of(Material.PAPER)
                            .name("${CC.D_GREEN}Rematch ${
                                UUID.fromString(rematchTargetID).username()
                            } ${CC.GRAY}(Right Click)")
                            .build()

                        val terminable = CompositeTerminable.create()
                        Events
                            .subscribe(PlayerInteractEvent::class.java)
                            .filter { event ->
                                event.hasItem() &&
                                    event.action.name.contains("RIGHT") &&
                                    event.item.isSimilar(rematchItem)
                            }
                            .handler {
                                DuelRequestPipeline.automateDuelRequestNoUI(
                                    player = player,
                                    target = UUID.fromString(rematchTargetID!!),
                                    kit = kit, map = map,
                                    region = rematchTargetRegion
                                )

                                Button.playNeutral(player)
                                terminable.closeAndReportException()
                            }
                            .bindWith(terminable)

                        Schedulers
                            .sync()
                            .runLater({
                                terminable.closeAndReportException()
                            }, 20L * 30L)
                            .bindWith(terminable)

                        Events
                            .subscribe(PlayerQuitEvent::class.java)
                            .filter { event ->
                                event.player.uniqueId == player.uniqueId
                            }
                            .handler {
                                terminable.closeAndReportException()
                            }
                            .bindWith(terminable)

                        terminable.with {
                            player.inventory.setItem(3, ItemStack(Material.AIR))
                        }

                        player.inventory.setItem(3, rematchItem)
                    }
                }
            }
            .bindWith(plugin)

        idlePreset.addSlot(
            7,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.WATCH)
                    .name("${CC.D_PURPLE}Navigator ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    PlayerMainMenu().openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.IRON_SWORD)
                    .name("${CC.GREEN}Play Casual ${CC.GRAY}(Right Click)")
                    .setUnbreakable(true)
            ).also {
                it.onClick = context@{ player ->
                    if (player.hasMetadata("frozen"))
                    {
                        player.sendMessage("${CC.RED}You cannot join queues while frozen!")
                        return@context
                    }

                    JoinQueueMenu(QueueType.Casual, 1).openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            1,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.DIAMOND_SWORD)
                    .name("${CC.AQUA}Play Ranked ${CC.GRAY}(Right Click)")
                    .setUnbreakable(true)
            ).also {
                it.onClick = context@{ player ->
                    val profile = PracticeProfileService.find(player)
                        ?: return@context

                    val configuration = PracticeConfigurationService.cached()
                    if (!player.isOp && !configuration.rankedQueueEnabled)
                    {
                        player.sendMessage("${CC.RED}Ranked queues are temporarily disabled. Please try again later.")
                        return@context
                    }

                    if (player.hasMetadata("frozen"))
                    {
                        player.sendMessage("${CC.RED}You cannot join queues while frozen!")
                        return@context
                    }

                    if (
                        !player.hasPermission("practice.bypass-ranked-queue-requirements")
                        && profile.globalStatistics.totalWins < configuration.minimumWinRequirement()
                    )
                    {
                        player.sendMessage(
                            "${CC.RED}You must have at least ${
                                configuration.minimumWinRequirement()
                            } wins to queue for a Ranked kit! You currently have ${CC.BOLD}${
                                profile.globalStatistics.totalWins
                            }${CC.RED} wins."
                        )
                        return@context
                    }

                    if (profile.hasActiveRankedBan())
                    {
                        profile.deliverRankedBanMessage(player)
                        return@context
                    }

                    JoinQueueMenu(QueueType.Ranked, 1).openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            5,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.NAME_TAG)
                    .name("${CC.PINK}Create a Party ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    player.performCommand("party create")
                    Button.playNeutral(player)

                    val lobbyPlayer = LobbyPlayerService.find(player)
                        ?: return@scope

                    synchronized(lobbyPlayer.stateUpdateLock) {
                        lobbyPlayer.state = PlayerState.InParty
                        lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
                    }
                }
            }
        )

        idlePreset.addSlot(
            6,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.ITEM_FRAME)
                    .name("${CC.YELLOW}View Leaderboards ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    LeaderboardsMenu().openMenu(player)
                    Button.playNeutral(player)
                }
            }
        )

        idlePreset.addSlot(
            2,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.BOOK)
                    .name("${CC.D_AQUA}Kit Editor ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = context@{ player ->
                    val profile = PracticeProfileService.find(player)
                        ?: return@context

                    EditorKitSelectionMenu(profile).openMenu(player)
                    Button.playNeutral(player)
                }
            }
        )

        idlePreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.REDSTONE_COMPARATOR)
                    .name("${CC.D_PURPLE}Settings ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    SettingMenu(player).openMenu(player)
                    Button.playNeutral(player)
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("idle", idlePreset)
        hotbarCache[PlayerState.Idle] = idlePreset

        val inQueuePreset = HotbarPreset()
        inQueuePreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(XMaterial.RED_DYE)
                    .name("${CC.RED}Leave Queue ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    QueueService.leaveQueue(player)
                    Button.playNeutral(player)
                    player.sendMessage(
                        "${CC.RED}You left the queue!"
                    )
                }
            }
        )

        inQueuePreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.BOOK)
                    .name("${CC.D_AQUA}Kit Editor ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    val profile = PracticeProfileService.find(player)
                        ?: return@scope

                    EditorKitSelectionMenu(profile).openMenu(player)
                    Button.playNeutral(player)
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("inQueue", inQueuePreset)
        hotbarCache[PlayerState.InQueue] = inQueuePreset

        val inPartyPreset = HotbarPreset()
        inPartyPreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(XMaterial.RED_DYE)
                    .name("${CC.RED}Leave Party ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    player.performCommand("party leave")
                    Button.playNeutral(player)

                    val lobbyPlayer = LobbyPlayerService.find(player)
                        ?: return@scope

                    synchronized(lobbyPlayer.stateUpdateLock) {
                        lobbyPlayer.state = PlayerState.Idle
                        lobbyPlayer.maintainStateTimeout = System.currentTimeMillis() + 1000L
                    }
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("inParty", inPartyPreset)
        hotbarCache[PlayerState.InParty] = inPartyPreset

        val inTournamentPreset = HotbarPreset()
        inTournamentPreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(XMaterial.RED_DYE)
                    .name("${CC.RED}Leave Tournament ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    Button.playNeutral(player)

                    TournamentCommand
                        .onLeave(
                            ScalaPlayer(
                                player = player,
                                audiences = audiences,
                                plugin = plugin
                            )
                        )
                }
            }
        )

        inTournamentPreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.BOOK)
                    .name("${CC.D_AQUA}Kit Editor ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    val profile = PracticeProfileService.find(player)
                        ?: return@scope

                    EditorKitSelectionMenu(profile).openMenu(player)
                    Button.playNeutral(player)
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("inTournament", inTournamentPreset)
        hotbarCache[PlayerState.InTournament] = inTournamentPreset

        Events
            .subscribe(
                PlayerJoinEvent::class.java,
                EventPriority.LOW
            )
            .handler { event ->
                idlePreset.applyToPlayer(event.player)
            }
            .bindWith(plugin)
    }

    fun get(state: PlayerState) = hotbarCache[state]!!

    fun reset(player: Player)
    {
        val lobbyPlayer = LobbyPlayerService.find(player.uniqueId)

        if (lobbyPlayer != null)
        {
            val state = lobbyPlayer.state
            val hotbar = get(state)

            if (!Bukkit.isPrimaryThread())
            {
                Tasks.sync {
                    hotbar.applyToPlayer(player)
                    return@sync
                }
            } else
            {
                hotbar.applyToPlayer(player)
            }
        }
    }
}
