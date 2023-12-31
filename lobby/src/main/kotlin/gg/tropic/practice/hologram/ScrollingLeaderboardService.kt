package gg.tropic.practice.hologram

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import org.bukkit.event.player.PlayerQuitEvent

/**
 * @author GrowlyX
 * @since 12/29/2023
 */
@Service
object ScrollingLeaderboardService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    @Configure
    fun configure()
    {
        Serializers.create {
            registerTypeAdapter(
                AbstractScrollingLeaderboard::class.java,
                AbstractTypeSerializer<AbstractScrollingLeaderboard>()
            )
        }

        EntityHandler
            .getEntitiesOfType<AbstractScrollingLeaderboard>()
            .forEach {
                it.secondsUntilRefresh = it.scrollTime
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                EntityHandler
                    .getEntitiesOfType<AbstractScrollingLeaderboard>()
                    .forEach { leaderboard ->
                        leaderboard.invalidateCacheEntries(it.player)
                    }
            }

        Schedulers
            .async()
            .runRepeating({ _ ->
                EntityHandler
                    .getEntitiesOfType<AbstractScrollingLeaderboard>()
                    .forEach {
                        it.secondsUntilRefresh = it
                            .secondsUntilRefresh!!
                            .minus(1)

                        if (it.secondsUntilRefresh!! <= 0)
                        {
                            val currentReference = it.currentReference
                            it.secondsUntilRefresh = it.scrollTime
                            it.currentReference = it.getNextReference(currentReference)
                        }
                    }
            }, 0L, 20L)
            .bindWith(plugin)
    }
}
