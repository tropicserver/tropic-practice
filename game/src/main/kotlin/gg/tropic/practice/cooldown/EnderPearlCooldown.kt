package gg.tropic.practice.cooldown

import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.cooldown.CooldownHandler
import gg.scala.lemon.cooldown.CooldownHandler.notifyAndContinueNoBypass
import gg.scala.lemon.cooldown.type.PlayerStaticCooldown
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.GameService
import gg.tropic.practice.kit.feature.FeatureFlag
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.round

/**
 * @author GrowlyX
 * @since 11/21/2021
 */
@Service
object EnderPearlCooldown : PlayerStaticCooldown(
    "Enderpearl", 0L
)
{
    @Inject
    lateinit var plugin: PracticeGame

    override fun durationFor(t: Player): Long
    {
        val game = GameService.byPlayer(t)
            ?: return 0L

        val duration = game.kit
            .featureConfig(
                FeatureFlag.EnderPearlCooldown,
                "duration"
            )

        return TimeUnit.SECONDS.toMillis(
            duration.toLong() + 1L
        )
    }

    @Configure
    fun configure()
    {
        // We need this for the exp bar
        notifyOnExpiration()
        whenExpired { } // do nothing

        Schedulers
            .async()
            .runRepeating(Runnable {
                for (id in tasks.keys)
                {
                    val player = Bukkit.getPlayer(id)
                        ?: continue

                    val time = fetchRemaining(player)
                    val seconds = round(time.toDouble() / 1000.0).toInt()

                    player.level = seconds
                    player.exp = max(
                        (time.toFloat() / 15000.0f).toDouble(),
                        0.0
                    ).toFloat() // math max to 0 to avoid negative.
                }
            }, 0L, 1L)

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter {
                it.hasItem() &&
                    it.action.name.contains("RIGHT") &&
                    it.item.type == Material.ENDER_PEARL
            }
            .handler {
                val player = it.player

                if (!notifyAndContinueNoBypass(this.javaClass, player, "throwing an enderpearl"))
                {
                    it.isCancelled = true
                    player.updateInventory()
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(ProjectileLaunchEvent::class.java)
            .filter { it.entity is EnderPearl }
            .filter { it.entity.shooter is Player }
            .handler {
                val player = it.entity.shooter as Player

                if (!it.isCancelled)
                {
                    val game = GameService.byPlayer(player)
                        ?: return@handler

                    if (game.flag(FeatureFlag.EnderPearlCooldown))
                    {
                        addOrOverride(player)
                    }
                }
            }
            .bindWith(plugin)

        CooldownHandler.register(this)
    }
}
