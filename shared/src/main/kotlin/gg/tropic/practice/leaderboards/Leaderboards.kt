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
    ELO("Ranked ELO", true),
    CasualWins("Casual Wins"),
    RankedWins("Ranked Wins", true),
    CasualWinStreak("Casual Daily Win Streak"),
    RankedWinStreak("Ranked Daily Win Streak", true);

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

data class Position(
    val uniqueId: UUID,
    val score: Long,
    val position: Long
)

data class ScoreUpdates(
    val oldScore: Long,
    val oldPosition: Long,
    val newPosition: Long,
    val nextPosition: Position?
)
{
    fun requiredScore() = nextPosition!!.score - oldScore
}
