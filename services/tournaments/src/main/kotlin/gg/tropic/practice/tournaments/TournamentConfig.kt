package gg.tropic.practice.tournaments

import kotlin.math.max

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
data class TournamentConfig(
    val teamSize: Int,
    val maxPlayers: Int,
    val kitID: String,
)
{
    fun startRequirement() = max(teamSize * 2, maxPlayers / 4)
}
