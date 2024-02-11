package gg.tropic.practice.player.prevention

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.menu.editor.AllowRemoveItemsWithinInventory
import me.lucko.helper.Events
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.bukkit.ItemUtils
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerBucketEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerPickupItemEvent

@Service
object PreventionListeners
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Configure
    fun configure()
    {
        listOf(
            PlayerItemConsumeEvent::class,
            ProjectileLaunchEvent::class,
            PlayerFishEvent::class,
            PlayerBucketEvent::class,
            PlayerLeashEntityEvent::class,
            PlayerPickupItemEvent::class
        ).forEach {
            Events.subscribe(it.java)
                .handler { event ->
                    event.isCancelled = true
                }
                .bindWith(plugin)
        }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .handler {
                it.setUseInteractedBlock(Event.Result.DENY)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerDropItemEvent::class.java)
            .handler {
                val notContentEditor = Menu
                    .currentlyOpenedMenus[it.player.uniqueId] !is AllowRemoveItemsWithinInventory

                if (notContentEditor)
                {
                    it.isCancelled = true
                    return@handler
                }

                it.itemDrop.remove()
            }
            .bindWith(plugin)

        Events
            .subscribe(InventoryClickEvent::class.java)
            .filter {
                it.clickedInventory == it.whoClicked.inventory &&
                    (it.slotType == InventoryType.SlotType.ARMOR ||
                        (runCatching { ItemUtils.itemTagHasKey(it.currentItem, "invokerc") }.getOrDefault(false)))
            }
            .handler {
                it.isCancelled = true
                it.cursor = null
            }
            .bindWith(plugin)

        Events
            .subscribe(InventoryClickEvent::class.java)
            .filter {
                it.clickedInventory == it.whoClicked.inventory
                    && it.click.isKeyboardClick
            }
            .handler {
                it.isCancelled = Menu.currentlyOpenedMenus[it.inventory.viewers.first().uniqueId] !is AllowRemoveItemsWithinInventory
            }
            .bindWith(plugin)

        Events
            .subscribe(InventoryDragEvent::class.java)
            .handler {
                val notContentEditor = Menu
                    .currentlyOpenedMenus[it.viewers.first().uniqueId] !is AllowRemoveItemsWithinInventory

                if (notContentEditor)
                {
                    it.isCancelled = true
                    return@handler
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(InventoryMoveItemEvent::class.java)
            .handler {
                val notContentEditor = Menu
                    .currentlyOpenedMenus[it.initiator.viewers.first().uniqueId] !is AllowRemoveItemsWithinInventory

                if (notContentEditor)
                {
                    it.isCancelled = true
                    return@handler
                }
            }
            .bindWith(plugin)


        listOf(EntityDamageEvent::class, EntityDamageByBlockEvent::class, EntityDamageByEntityEvent::class)
            .forEach { damageEvent ->
                Events
                    .subscribe(damageEvent.java)
                    .handler {
                        if (it.cause == EntityDamageEvent.DamageCause.VOID)
                        {
                            it.entity.teleport(
                                PracticeConfigurationService.cached().spawnLocation
                                    .toLocation(it.entity.world)
                            )
                        }

                        it.isCancelled = true
                    }
                    .bindWith(plugin)
            }

        listOf(
            BlockPlaceEvent::class,
            BlockBreakEvent::class,
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
