package gg.tropic.practice.tournaments

import gg.tropic.practice.region.Region
import java.util.*

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
data class TournamentConfig(
    val creator: UUID,
    val teamSize: Int,
    val maxPlayers: Int,
    val kitID: String,
    val region: Region
)
