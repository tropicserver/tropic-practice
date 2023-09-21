package gg.tropic.practice.kit.statistics

import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
open class KitStatistics
{
    var plays: Int = 0
    var wins: Int = 0
    var kills: Int = 0
    var deaths: Int = 0

    var longestStreak: Int = 0
        private set

    var streak: Volatile<Int> = Volatile(
        defaultValue = 0,
        lifetime = TimeUnit.DAYS.toMillis(1)
    )

    @Transient
    var backingApplyStreakUpdates: ApplyUpdates<Int>? = null
        get()
        {
            if (field == null)
            {
                field = ApplyUpdates(listOf({
                    streak /= it
                }, {
                    if (it > longestStreak)
                    {
                        longestStreak = it
                    }
                }))
            }

            return field!!
        }

    val applyStreakUpdates: ApplyUpdates<Int>
        get() = backingApplyStreakUpdates!!
}
