package gg.tropic.practice.player.hotbar

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import gg.scala.lemon.redirection.expectation.PlayerJoinWithExpectationEvent
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.commands.TournamentCommand
import gg.tropic.practice.configuration.LobbyConfigurationService
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.menu.JoinQueueMenu
import gg.tropic.practice.menu.LeaderboardsMenu
import gg.tropic.practice.menu.PlayerMainMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.queue.QueueType
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
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

        data class RematchData(
            val kitID: String,
            val queueType: QueueType
        )

        val rematches = mutableMapOf<UUID, RematchData>()
        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                rematches.remove(it.player.uniqueId)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                Tasks.delayed(10L) {
                    if (rematches[it.player.uniqueId] != null)
                    {
                        return@delayed
                    }

                    if (!it.player.isOnline)
                    {
                        return@delayed
                    }

                    with(LobbyConfigurationService.cached()) {
                        if (loginMOTD.isNotEmpty())
                        {
                            loginMOTD.forEach(it.player::sendMessage)
                        }
                    }
                }
            }

        Events
            .subscribe(PlayerJoinWithExpectationEvent::class.java)
            .handler {
                if (it.response.parameters.containsKey("rematch-kit-id"))
                {
                    val rematchKitID = it.response.parameters["rematch-kit-id"]
                    val rematchQueueType = it.response.parameters["rematch-queue-type"]

                    val player = Bukkit.getPlayer(it.uniqueId)
                        ?: return@handler

                    rematches[it.uniqueId] = RematchData(
                        rematchKitID!!,
                        QueueType.valueOf(rematchQueueType!!)
                    )

                    reset(player)
                }
            }
            .bindWith(plugin)

        idlePreset.addSlot(
            2,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.WATCH)
                    .name("${CC.D_AQUA}Navigator ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    PlayerMainMenu().openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            3,
            DynamicHotbarPresetEntry()
                .apply {
                    onBuild = build@{
                        val rematchData = rematches[it.uniqueId]
                            ?: return@build ItemStack(Material.AIR)

                        val kit = KitService.cached().kits[rematchData.kitID]
                            ?: return@build ItemStack(Material.AIR)

                        ItemBuilder(Material.PAPER)
                            .name("${CC.SEC}Play ${CC.PRI}${rematchData.queueType.name} ${kit.displayName} ${CC.GRAY}(Right Click)")
                            .build()
                    }

                    onClick = click@{ player ->
                        val rematchData = rematches[player.uniqueId]
                            ?: return@click

                        val kit = KitService.cached().kits[rematchData.kitID]
                            ?: return@click

                        rematches.remove(player.uniqueId)
                        reset(player)

                        QueueService.joinQueue(
                            kit = kit,
                            queueType = rematchData.queueType,
                            teamSize = 1,
                            player = player
                        )

                        player.sendMessage(
                            "${CC.GREEN}You have joined the ${CC.PRI}${rematchData.queueType.name} 1v1 ${kit.displayName}${CC.GREEN} queue!"
                        )
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
                it.onClick = { player ->
                    JoinQueueMenu(QueueType.Casual, 1).openMenu(player)
//                    CasualQueueSelectSizeMenu().openMenu(player)
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

                    val configuration = LobbyConfigurationService.cached()
                    if (!configuration.rankedQueueEnabled)
                    {
                        player.sendMessage("${CC.RED}Ranked queues are temporarily disabled. Please try again later.")
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
                it.onClick = { player ->
                    player.sendMessage("${CC.RED}Parties are coming soon!")
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
                it.onClick = { player ->
                    val profile = PracticeProfileService.find(player)

                    if (profile != null)
                    {
                        EditorKitSelectionMenu(profile).openMenu(player)
                    }
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("inQueue", inQueuePreset)
        hotbarCache[PlayerState.InQueue] = inQueuePreset

        val inTournamentPreset = HotbarPreset()
        inTournamentPreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(XMaterial.RED_DYE)
                    .name("${CC.RED}Leave Tournament ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = scope@{ player ->
                    TournamentCommand
                        .onLeave(
                            ScalaPlayer(
                                player = player,
                                audiences = audiences,
                                plugin = plugin
                            )
                        )
                        .join()
                }
            }
        )

        inTournamentPreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.BOOK)
                    .name("${CC.D_AQUA}Kit Editor ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    val profile = PracticeProfileService.find(player)

                    if (profile != null)
                    {
                        EditorKitSelectionMenu(profile).openMenu(player)
                    }
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
