package gg.tropic.practice.queue

import gg.tropic.practice.games.QueueType

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class QueueState(
    val kitId: String,
    val queueType: QueueType,
    val teamSize: Int,
    val joined: Long
)
