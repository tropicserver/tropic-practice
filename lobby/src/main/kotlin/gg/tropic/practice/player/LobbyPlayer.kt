package gg.tropic.practice.player

import java.util.UUID

data class LobbyPlayer(
    val uniqueId: UUID,
    val state: PlayerState = PlayerState.Idle
)
