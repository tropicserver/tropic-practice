package gg.tropic.practice.tournaments

import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
class Tournament(val config: TournamentConfig) : () -> Unit
{
    private var state = TournamentState.Populating
    private val matches = mutableSetOf<UUID>()

    fun populatingPeriodics()
    {

    }

    fun populating()
    {
        populatingPeriodics()
    }

    fun inRound()
    {

    }

    fun roundStarting()
    {

    }

    fun ending()
    {

    }

    override fun invoke() = when (state)
    {
        TournamentState.Populating -> populating()
        TournamentState.InRound -> inRound()
        TournamentState.RoundStarting -> roundStarting()
        TournamentState.Ended -> ending()
    }
}
