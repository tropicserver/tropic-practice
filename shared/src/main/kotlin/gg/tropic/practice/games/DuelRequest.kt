package gg.tropic.practice.games

import java.util.UUID

/**
 * @author GrowlyX
 * @since 10/21/2023
 */
data class DuelRequest(
    val requester: UUID,
    val requestee: UUID,
    val kitID: String,
    val mapID: String? = null
)
