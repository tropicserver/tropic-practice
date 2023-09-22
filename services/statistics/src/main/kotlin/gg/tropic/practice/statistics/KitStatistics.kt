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

    var streak = SingleWeekLifetime(defaultValue = 0)

    fun strakUpdates() = ApplyUpdates<Int>(listOf({
        streak /= it
    }, {
        if (it > longestStreak)
        {
            longestStreak = it
        }
    }))
}
