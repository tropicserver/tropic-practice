package gg.tropic.practice.queue

import gg.tropic.practice.region.Region
import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class QueueEntry(
    val leader: UUID,
    val leaderPing: Int,
    val queueRegion: Region,
    val maxPingDiff: Int,
    val leaderRangedPing: MinMaxRangedNumber =
        MinMaxRangedNumber(
            med = leaderPing, diffsBy = 10
        ),
    var lastPingRangeExpansion: Long = System.currentTimeMillis(),
    var lastRecordedDifferential: Int = 0,
    val leaderELO: Int,
    val leaderRangedELO: MinMaxRangedNumber =
        MinMaxRangedNumber(
            med = leaderELO, diffsBy = 10
        ),
    var lastELORangeExpansion: Long = System.currentTimeMillis(),
    val players: List<UUID>,
    val joinQueueTimestamp: Long = System.currentTimeMillis(),
    var preferredQueueRegion: Region = queueRegion
)
