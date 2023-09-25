package gg.tropic.practice.application.api.defaults.game

import gg.tropic.practice.games.QueueType
import java.util.*

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class GameExpectation(
    val identifier: UUID,
    val players: List<UUID>,
    val teams: Map<GameTeamSide, GameTeam>,
    val kitId: String,
    val mapId: String,
    /**
     * Null queue types mean it is a private duel.
     */
    val queueType: QueueType? = null
)
