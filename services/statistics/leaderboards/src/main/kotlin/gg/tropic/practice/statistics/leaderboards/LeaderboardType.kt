package gg.tropic.practice.statistics.leaderboards

import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.statistics.ranked.RankedKitStatistics

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
enum class LeaderboardType(
    val displayName: String,
    val fromKit: ImmutablePracticeProfile.(ImmutableKit) -> Long?,
    val fromGlobal: ImmutablePracticeProfile.() -> Long?,
    val enforceRanked: Boolean = false
)
{
    ELO(
        displayName = "Ranked ELO",
        fromKit = {
            rankedStatistics[it.id]?.elo?.toLong()
        },
        fromGlobal = {
            val average = rankedStatistics.values
                .map(RankedKitStatistics::elo)
                .average()

            if (average.isNaN()) null else average.toLong()
        },
        enforceRanked = true
    )
}
