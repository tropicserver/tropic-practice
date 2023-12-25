package gg.tropic.practice.games

import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.statistics.KitStatistics

/**
 * @author GrowlyX
 * @since 10/25/2023
 */
fun PracticeProfile.useKitStatistics(
    game: GameImpl, block: KitStatistics.() -> Unit
)
{
    when (game.expectationModel.queueType)
    {
        QueueType.Casual -> block(
            getCasualStatsFor(game.kit)
        )
        QueueType.Ranked -> block(
            getRankedStatsFor(game.kit)
        )
        null -> {}
    }
}
