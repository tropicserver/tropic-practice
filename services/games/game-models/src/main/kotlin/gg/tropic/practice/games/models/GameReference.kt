package gg.tropic.practice.games.models

import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
data class GameReference(
    val uniqueId: UUID,
    val mapID: String,
    val kitID: String,
    val state: String, // TODO: Enum
    val replicationID: String,
    val server: String,
    val players: List<UUID>,
    val spectators: List<UUID>
)
