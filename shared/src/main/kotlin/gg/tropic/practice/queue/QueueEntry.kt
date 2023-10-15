package gg.tropic.practice.queue

import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class QueueEntry(
    val leader: UUID,
    val leaderPing: Int,
    val defaultPingDiff: Int,
    val leaderRangedPing: MinMaxRangedNumber =
        MinMaxRangedNumber(
            med = leaderPing, diffsBy = defaultPingDiff
        ),
    val leaderELO: Int,
    val defaultELODiff: Int,
    val leaderRangedELO: MinMaxRangedNumber =
        MinMaxRangedNumber(
            med = leaderELO, diffsBy = defaultELODiff
        ),
    val players: List<UUID>
)
