package gg.tropic.practice.leaderboards

import java.util.*

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
enum class ReferenceLeaderboardType(
    val displayName: String,
    val enforceRanked: Boolean = false
)
{
    ELO("Ranked ELO"),
    CasualWins("Casual Wins"),
    RankedWins("Ranked Wins", true),
    CasualWinStreak("Casual Daily Wins"),
    RankedWinStreak("Ranked Daily Wins", true);

    fun previous(): ReferenceLeaderboardType
    {
        return entries
            .getOrNull(ordinal - 1)
            ?: entries.last()
    }

    fun next(): ReferenceLeaderboardType
    {
        return entries
            .getOrNull(ordinal + 1)
            ?: entries.first()
    }
}

data class Reference(
    val leaderboardType: ReferenceLeaderboardType,
    val kitID: String?
)
{
    fun id() = "${leaderboardType.name}:${kitID ?: "global"}"
}

data class LeaderboardReferences(
    val references: List<Reference>
)

data class LeaderboardEntry(
    val uniqueId: UUID,
    val value: Long
)
