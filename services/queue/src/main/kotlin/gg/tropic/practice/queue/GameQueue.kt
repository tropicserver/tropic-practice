package gg.tropic.practice.queue

import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.games.QueueType
import kotlin.concurrent.thread

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
class GameQueue(
    val kit: ImmutableKit,
    private val queueType: QueueType,
    private val teamSize: Int
) : () -> Unit
{
    private var thread: Thread? = null

    fun queueId() = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"

    fun start()
    {
        check(thread == null)
        thread = thread(
            isDaemon = true,
            name = "queues-${queueId()}",
            block = this
        )
    }

    fun destroy()
    {
        checkNotNull(thread)
        thread?.interrupt()
        thread = null
    }

    override fun invoke()
    {
        while (true)
        {

        }
    }
}
