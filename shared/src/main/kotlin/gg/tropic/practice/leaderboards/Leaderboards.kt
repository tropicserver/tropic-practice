package gg.tropic.practice.leaderboards

import java.util.*

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
enum class ReferenceLeaderboardType
{
    ELO, CasualWins, RankedWins, CasualWinStreak, RankedWinStreak
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
