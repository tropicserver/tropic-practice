package gg.tropic.practice.leaderboards

import gg.tropic.practice.games.QueueType
import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
enum class ReferenceLeaderboardType
{
    ELO, CasualWins, RankedWins, CasualWinStreak, RankedWinStreak
}

data class Reference(
    val queueType: QueueType,
    val leaderboardType: ReferenceLeaderboardType,
    val kitID: String?
)
{
    fun id() = "${queueType.name}:${leaderboardType.name}:${kitID ?: "global"}"
}

data class LeaderboardReferences(
    val references: List<Reference>
)

data class LeaderboardEntry(
    val uniqueId: UUID,
    val value: Long
)
