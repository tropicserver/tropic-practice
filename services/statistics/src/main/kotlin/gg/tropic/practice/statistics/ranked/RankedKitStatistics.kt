package gg.tropic.practice.statistics.ranked

import gg.tropic.practice.statistics.ApplyUpdates
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.SingleDayLifetime
import gg.tropic.practice.statistics.SingleWeekLifetime

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

    val dailyEloChange = SingleDayLifetime(defaultValue = 0)
    val weeklyEloChange = SingleWeekLifetime(defaultValue = 0)

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
