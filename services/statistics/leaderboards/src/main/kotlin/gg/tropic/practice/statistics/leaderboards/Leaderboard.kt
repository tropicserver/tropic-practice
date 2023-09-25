package gg.tropic.practice.statistics.leaderboards

import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import java.util.logging.Logger
import kotlin.concurrent.thread

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
class Leaderboard(
    private val type: LeaderboardType,
    private val kit: ImmutableKit? = null
) : () -> Unit
{
    private var thread: Thread? = null

    fun leaderboardId() = "${type.name}-${kit?.id ?: "global"}"

    fun start()
    {
        check(thread == null)
        thread = thread(
            isDaemon = true,
            name = "leaderboards-${type.name}-${kit?.id ?: "global"}",
            block = this
        )

        Logger.getGlobal()
            .info(
                "Building a leaderboard with id: ${leaderboardId()}"
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

    }
}
