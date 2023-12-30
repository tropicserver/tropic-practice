package gg.tropic.practice.hologram

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import me.lucko.helper.Schedulers
import net.evilblock.cubed.entity.EntityHandler

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
        EntityHandler
            .getEntitiesOfType<ScrollingTypeLeaderboardHologram>()
            .forEach {
                it.secondsUntilRefresh = it.scrollTime
            }

        EntityHandler
            .getEntitiesOfType<ScrollingKitLeaderboardHologram>()
            .forEach {
                it.secondsUntilRefresh = it.scrollTime
            }

        Schedulers
            .async()
            .runRepeating({ _ ->
                EntityHandler
                    .getEntitiesOfType<ScrollingTypeLeaderboardHologram>()
                    .forEach {
                        // Expecting a non-null value every timee
                        it.secondsUntilRefresh = it
                            .secondsUntilRefresh!!
                            .minus(1)

                        if (it.secondsUntilRefresh!! <= 0)
                        {
                            it.secondsUntilRefresh = it.scrollTime
                            it.state = ReferenceLeaderboardType.valueOf(
                                it.scrollStates
                                    .getOrNull(
                                        it.scrollStates.indexOf(it.state!!.name) + 1
                                    )
                                    ?: it.scrollStates.first()
                            )
                        }
                    }

                EntityHandler
                    .getEntitiesOfType<ScrollingKitLeaderboardHologram>()
                    .forEach {
                        // Expecting a non-null value every timee
                        it.secondsUntilRefresh = it
                            .secondsUntilRefresh!!
                            .minus(1)

                        if (it.secondsUntilRefresh!! <= 0)
                        {
                            it.secondsUntilRefresh = it.scrollTime
                            it.state = it.kits
                                .getOrNull(
                                    it.kits.indexOf(it.state!!) + 1
                                )
                                ?: it.kits.first()
                        }
                    }
            }, 0L, 20L)
            .bindWith(plugin)
    }
}
