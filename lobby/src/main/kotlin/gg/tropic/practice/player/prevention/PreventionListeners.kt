package gg.tropic.practice.player.prevention

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import me.lucko.helper.Events
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerLevelChangeEvent

@Service
object PreventionListeners
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Configure
    fun configure()
    {
        listOf(
            BlockPlaceEvent::class,
            BlockBreakEvent::class,
            EntityDamageByBlockEvent::class,
            EntityDamageByEntityEvent::class,
            EntityDamageEvent::class,
            PlayerDropItemEvent::class,
            FoodLevelChangeEvent::class
        ).forEach { event ->
            Events.subscribe(event.java)
                .handler {
                    it.isCancelled = true
                }.bindWith(plugin)
        }

    }
}