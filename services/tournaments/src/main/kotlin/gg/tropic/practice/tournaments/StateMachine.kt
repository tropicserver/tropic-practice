package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.queue.GameQueueManager
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
sealed class TournamentState
{
    data object Populating : TournamentState()
    data object RoundStarting : TournamentState()
    data object RoundInProgress : TournamentState()
    data object Ended : TournamentState()
}

sealed class StateEvent
{
    data object OnPopulated : StateEvent()
    data object OnRoundsStarted : StateEvent()
    data object OnRoundsEnded : StateEvent()
}

sealed class SideEffect : (Tournament) -> Unit
{
    data object DetermineNextMatchList : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {

        }
    }

    data object InvokeStartOnMatchList : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {
            CompletableFuture
                .allOf(
                    *(tournament.currentMatchList.map {
                        val map = MapDataSync.cached().maps[it.mapId]
                            ?: return@map CompletableFuture
                                .completedFuture(null)

                        GameQueueManager.prepareGameFor(
                            map = map,
                            expectation = it,
                            region = tournament.config.region,
                            cleanup = { }
                        )
                    }.toTypedArray())
                )
                .join()
        }
    }

    data object FinalizeTournamentAndDispose : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {
            tournament.stop()
        }
    }
}
