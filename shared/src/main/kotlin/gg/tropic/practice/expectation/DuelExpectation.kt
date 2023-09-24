package gg.tropic.practice.expectation

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import java.util.*

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
data class DuelExpectation(
    override val identifier: UUID,
    val players: List<UUID>,
    val teams: Map<GameTeamSide, GameTeam>,
    val kitId: String,
    val mapId: String,
    /**
     * Null queue types mean it is a private duel.
     */
    val queueType: QueueType? = null
) : IDataStoreObject
