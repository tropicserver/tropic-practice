package gg.tropic.practice.player.hotbar

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.menu.CasualQueueSelectSizeMenu
import gg.tropic.practice.menu.JoinQueueMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueService
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent

@Service
object LobbyHotbarService
{
    @Inject
    lateinit var plugin: PracticeLobby

    private val hotbarCache = mutableMapOf<PlayerState, HotbarPreset>()

    @Configure
    fun configure()
    {
        val idlePreset = HotbarPreset()

        idlePreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.IRON_SWORD)
                    .name("${CC.GREEN}Play Casual ${CC.GRAY}(Right Click)")
                    .setUnbreakable(true)
            ).also {
                it.onClick = { player ->
                    CasualQueueSelectSizeMenu().openMenu(player)
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
                it.onClick = { player ->
                    JoinQueueMenu(QueueType.Ranked, 1).openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            2,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.BLAZE_POWDER)
                    .name("${CC.GOLD}Cosmetics ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player ->
                    player.performCommand("cosmetics")
                }
            }
        )

        idlePreset.addSlot(
            4,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.NETHER_STAR)
                    .name("${CC.PINK}Create a Party ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player -> }
            }
        )

        idlePreset.addSlot(
            6,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.QUARTZ)
                    .name("${CC.YELLOW}Leaderboards ${CC.GRAY}(Right Click)")
            ).also {
                it.onClick = { player -> }
            }
        )


        idlePreset.addSlot(
            7,
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

        idlePreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(Material.REDSTONE_COMPARATOR)
                    .name("${CC.D_PURPLE}Edit Settings ${CC.GRAY}(Right Click)")
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

        HotbarPresetHandler.startTrackingHotbar("inQueue", inQueuePreset)
        hotbarCache[PlayerState.InQueue] = inQueuePreset

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
