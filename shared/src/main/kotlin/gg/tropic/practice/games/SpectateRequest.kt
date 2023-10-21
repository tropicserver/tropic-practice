package gg.tropic.practice.games

import java.util.UUID

/**
 * @author GrowlyX
 * @since 10/20/2023
 */
data class SpectateRequest(
    val player: UUID,
    val target: UUID,
    val bypassesSpectatorAllowanceChecks: Boolean = false
)
