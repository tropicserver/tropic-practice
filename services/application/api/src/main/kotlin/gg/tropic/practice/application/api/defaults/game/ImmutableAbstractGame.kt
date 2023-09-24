package gg.tropic.practice.application.api.defaults.game

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import java.util.*

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
enum class GameTeamSide
{
    A, B
}

class GameTeam(
    val side: GameTeamSide,
    val players: List<UUID>
)

class ImmutableAbstractGame(
    val expectation: UUID,
    val teams: Map<GameTeamSide, GameTeam>,
    val kit: ImmutableKit
) : IDataStoreObject
{
    override val identifier: UUID
        get() = this.expectation

    var startTimestamp = -1L
}
