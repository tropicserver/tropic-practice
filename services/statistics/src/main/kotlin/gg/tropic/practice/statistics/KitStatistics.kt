package gg.tropic.practice.statistics

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
open class KitStatistics
{
    var plays = 0
    var wins = 0
    var kills = 0
    var deaths = 0

    var longestStreak = 0
        private set

    var streak = 0
        private set
    var dailyStreak = SingleDayLifetime(defaultValue = 0)

    fun streakUpdates() = ApplyUpdates<Boolean>(listOf({
        dailyStreak /= if (it) dailyStreak() + 1 else 0
    }, {
       if (it)
       {
           streak += 1
       } else
       {
           streak = 0
       }
    }, {
        if (it)
        {
            if (streak > longestStreak)
            {
                longestStreak = streak
            }
        }
    }))
}
