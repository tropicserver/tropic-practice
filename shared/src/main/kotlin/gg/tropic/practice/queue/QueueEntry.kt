package gg.tropic.practice.queue

import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class QueueEntry(
    val leader: UUID,
    val leaderPing: Int,
    val players: List<UUID>,
    val averageELO: Int? = null
)
