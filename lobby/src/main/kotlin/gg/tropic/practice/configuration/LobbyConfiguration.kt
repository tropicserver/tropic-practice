package gg.tropic.practice.configuration

import gg.tropic.practice.map.metadata.anonymous.Position

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
data class LobbyConfiguration(
    var spawnLocation: Position = Position(
        0.0, 100.0, 0.0, 180.0F, 0.0F
    )
)
