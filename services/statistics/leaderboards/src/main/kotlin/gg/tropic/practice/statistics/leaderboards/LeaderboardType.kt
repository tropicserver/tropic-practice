package gg.tropic.practice.statistics.leaderboards

import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
enum class LeaderboardType(
    val fromKit: ImmutablePracticeProfile.(ImmutableKit) -> Long?,
    val fromGlobal: ImmutablePracticeProfile.() -> Long?,
    val enforceRanked: Boolean = false
)
{
    ELO(
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
    ),
    CasualWins(
        fromKit = {
            casualStatistics[it.id]?.wins?.toLong()
        },
        fromGlobal = {
            casualStatistics.values
                .sumOf(KitStatistics::wins)
                .toLong()
        }
    ),
    RankedWins(
        fromKit = {
            rankedStatistics[it.id]?.wins?.toLong()
        },
        fromGlobal = {
            rankedStatistics.values
                .sumOf(KitStatistics::wins)
                .toLong()
        }
    ),
    CasualWinStreak(
        fromKit = context@{
            val volatile = casualStatistics[it.id]?.dailyStreak
                ?: return@context null

            if (volatile.isExpired()) null else volatile.getUnchecked().toLong()
        },
        fromGlobal = {
            val result = casualStatistics.maxOfOrNull {
                val volatile = casualStatistics[it.key]?.dailyStreak
                    ?: return@maxOfOrNull -1L

                if (volatile.isExpired())
                    -1L else volatile.getUnchecked().toLong()
            }

            if (result == -1L) null else result
        }
    ),
    RankedWinStreak(
        fromKit = context@{
            val volatile = rankedStatistics[it.id]?.dailyStreak
                ?: return@context null

            if (volatile.isExpired()) null else volatile.getUnchecked().toLong()
        },
        fromGlobal = {
            val result = rankedStatistics.maxOfOrNull {
                val volatile = rankedStatistics[it.key]?.dailyStreak
                    ?: return@maxOfOrNull -1L

                if (volatile.isExpired())
                    -1L else volatile.getUnchecked().toLong()
            }

            if (result == -1L) null else result
        },
        enforceRanked = true
    )
}
