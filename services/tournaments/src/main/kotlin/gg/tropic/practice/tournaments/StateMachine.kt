package gg.tropic.practice.tournaments

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

        }
    }

    data object FinalizeTournamentAndDispose : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {

        }
    }
}
