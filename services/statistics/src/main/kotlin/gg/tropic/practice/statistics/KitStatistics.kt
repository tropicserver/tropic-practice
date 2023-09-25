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

    fun strakUpdates() = ApplyUpdates<Int>(listOf({
        dailyStreak /= it
    }, {
        if (it > longestStreak)
        {
            longestStreak = it
        }
    }, {
        streak = it
    }))
}
