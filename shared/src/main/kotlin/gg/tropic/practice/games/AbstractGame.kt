package gg.tropic.practice.games

import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.kit.Kit
import java.util.*

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
abstract class AbstractGame(
    val expectation: UUID,
    val teams: Map<GameTeamSide, GameTeam>,
    val ladder: Kit
) : IDataStoreObject
{
    override val identifier: UUID
        get() = this.expectation

    var report: GameReport? = null
    var startTimestamp = -1L
}
