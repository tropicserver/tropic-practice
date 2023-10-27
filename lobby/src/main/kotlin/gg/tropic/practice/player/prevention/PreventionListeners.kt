package gg.tropic.practice.player.prevention

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.player.LobbyPlayerService
import me.lucko.helper.Events
import net.evilblock.cubed.util.bukkit.ItemUtils
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent

@Service
object PreventionListeners
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Configure
    fun configure()
    {
        Events
            .subscribe(PlayerInteractEvent::class.java)
            .handler {
                it.setUseInteractedBlock(Event.Result.DENY)
            }
            .bindWith(plugin)

        Events
            .subscribe(InventoryClickEvent::class.java)
            .filter {
                it.clickedInventory == it.whoClicked.inventory &&
                    (it.slotType == InventoryType.SlotType.ARMOR ||
                        ItemUtils.itemTagHasKey(it.currentItem, "invokerc"))
            }
            .handler {
                it.isCancelled = true
                it.cursor = null
            }
            .bindWith(plugin)

        listOf(
            BlockPlaceEvent::class,
            BlockBreakEvent::class,
            EntityDamageByBlockEvent::class,
            EntityDamageByEntityEvent::class,
            EntityDamageEvent::class,
            PlayerDropItemEvent::class,
            FoodLevelChangeEvent::class
        ).forEach { event ->
            Events
                .subscribe(event.java)
                .handler {
                    it.isCancelled = true
                }
                .bindWith(plugin)
        }
    }
}
