package gg.tropic.practice.games

import gg.tropic.practice.region.Region
import java.util.UUID

/**
 * @author GrowlyX
 * @since 10/21/2023
 */
data class DuelRequest(
    val requester: UUID,
    val requesterPing: Int,
    val requestee: UUID,
    val region: Region,
    val kitID: String,
    val mapID: String? = null
)
