package gg.tropic.practice.games.ranked

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
data class CalculationResult(
    val winnerOld: Int,
    val winnerGain: Int,
    val loserOld: Int,
    val loserGain: Int
)
{
    val winnerNew = winnerOld + winnerGain
    val loserNew = loserOld + loserGain
}
