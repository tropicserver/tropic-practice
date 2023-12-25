package gg.tropic.practice.queue

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
