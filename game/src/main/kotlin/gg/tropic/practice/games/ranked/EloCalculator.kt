package gg.tropic.practice.games.ranked

/**
 * @author GrowlyX
 * @since 8/20/2022
 */
interface EloCalculator
{
    fun getNewRating(winner: Int, loser: Int): CalculationResult
}
