package gg.tropic.practice.kit.statistics.ranked

import gg.tropic.practice.kit.statistics.ApplyUpdates
import gg.tropic.practice.kit.statistics.KitStatistics
import gg.tropic.practice.kit.statistics.Volatile
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class RankedKitStatistics : KitStatistics()
{
    var elo = 1000
        private set

    var highestElo = elo
        private set

    val dailyEloChange = Volatile(
        defaultValue = 0,
        lifetime = TimeUnit.DAYS.toMillis(1)
    )

    val weeklyEloChange = Volatile(
        defaultValue = 0,
        lifetime = TimeUnit.DAYS.toMillis(7)
    )

    @Transient
    private var backingApplyEloUpdates: ApplyUpdates<Int>? = null
        get()
        {
            if (field == null)
            {
                field = ApplyUpdates(listOf({
                    this.elo = it
                }, {
                    dailyEloChange /= dailyEloChange() + (it - elo)
                }, {
                    weeklyEloChange /= weeklyEloChange() + (it - elo)
                }, {
                    if (it > highestElo)
                    {
                        highestElo = it
                    }
                }))
            }

            return field!!
        }

    val applyEloUpdates: ApplyUpdates<Int>
        get() = backingApplyEloUpdates!!
}
