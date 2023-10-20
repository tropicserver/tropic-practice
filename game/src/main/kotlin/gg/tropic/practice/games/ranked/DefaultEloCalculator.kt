package gg.tropic.practice.games.ranked

import gg.scala.commons.annotations.inject.AutoBind
import java.util.*
import kotlin.math.pow

/**
 * JOGRE's implementation of the ELO rating system.  The following is an
 * example of how to use the Elo Rating System.
 * `
 * EloRatingSystem elo = new EloRatingSystem();
 * int userRating = 1600;
 * int opponentRating = 1650;
 * int newUserRating = elo.getNewRating(userRating, opponentRating, WIN);
 * int newOpponentRating = elo.getNewRating(opponentRating, userRating, LOSS);
` *
 *
 * @author Garrett Lehman (gman)
 */
@AutoBind
object DefaultEloCalculator : EloCalculator
{
    /**
     * Small inner class data structure to describe a KFactor range.
     */
    data class KFactor(val startIndex: Int, val endIndex: Int, val value: Double)

    private var kFactors = arrayOf<KFactor?>()

    init
    {
        // Read k factor in from server properties
        val kFactorStr = "0-4000=32"

        // Split each of the kFactor ranges up (kfactor1,factor2, etc)
        val st1 = StringTokenizer(kFactorStr, ",")
        kFactors = arrayOfNulls(st1.countTokens())

        var index = 0
        while (st1.hasMoreTokens())
        {
            val kfr = st1.nextToken()

            // Split the range from the value (range=value)
            var st2 = StringTokenizer(kfr, "=")
            val range = st2.nextToken()

            // Retrieve value
            val value = st2.nextToken().toDouble()

            // Retrieve start end index from the range
            st2 = StringTokenizer(range, "-")
            val startIndex = st2.nextToken().toInt()
            val endIndex = st2.nextToken().toInt()

            // Add kFactor to range
            kFactors[index++] = KFactor(startIndex, endIndex, value)
        }
    }

    override fun getNewRating(
        player: Int, opponent: Int, change: EloChange
    ): Int
    {
        return when (change)
        {
            EloChange.WIN -> getNewRating(player, opponent, 1.0)
            EloChange.LOSS -> getNewRating(player, opponent, -1.0)
            EloChange.DRAW -> getNewRating(player, opponent, 0.5)
        }
    }

    /**
     * Get new rating.
     *
     * @param rating         Rating of either the current player or the average of the
     * current team.
     * @param opponentRating Rating of either the opponent player or the average of the
     * opponent team or teams.
     * @param score          Score: 0=Loss 0.5=Draw 1.0=Win
     * @return the new rating
     */
    private fun getNewRating(rating: Int, opponentRating: Int, score: Double): Int
    {
        val kFactor = getKFactor(rating)
        val expectedScore = getExpectedScore(rating, opponentRating)

        return calculateNewRating(rating, score, expectedScore, kFactor)
    }

    /**
     * Calculate the new rating based on the ELO standard formula.
     * newRating = oldRating + constant * (score - expectedScore)
     *
     * @param oldRating     Old Rating
     * @param score         Score
     * @param expectedScore Expected Score
     * @return the new rating of the player
     */
    private fun calculateNewRating(oldRating: Int, score: Double, expectedScore: Double, kFactor: Double): Int
    {
        return oldRating + (kFactor * (score - expectedScore)).toInt()
    }

    /**
     * This is the standard chess constant. This constant can differ
     * based on different games.  The higher the constant, the faster
     * the rating will grow.  That is why for this standard chess method,
     * the constant is higher for weaker players and lower for stronger
     * players.
     *
     * @param rating Rating
     * @return Constant
     */
    private fun getKFactor(rating: Int): Double
    {
        // Return the correct k factor.
        for (kFactor in kFactors)
        {
            if (rating >= kFactor!!.startIndex && rating <= kFactor.endIndex)
            {
                return kFactor.value
            }
        }

        return 32.0
    }

    /**
     * Get expected score based on two players.  If more than two players
     * are competing, then opponentRating will be the average of all other
     * opponent's ratings.  If there is two teams against each other, rating
     * and opponentRating will be the average of those players.
     *
     * @param rating         Rating
     * @param opponentRating Opponent(s) rating
     * @return the expected score
     */
    private fun getExpectedScore(rating: Int, opponentRating: Int): Double
    {
        return 1.0 / (1.0 + 10.0.pow((opponentRating - rating).toDouble() / 400.0))
    }
}
