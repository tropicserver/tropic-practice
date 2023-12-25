package gg.tropic.practice.tournaments

import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/18/2023
 */
data class TournamentMember(
    val leader: UUID,
    val players: Set<UUID>
)
