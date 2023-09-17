package gg.minequest.duels.shared.game

import gg.minequest.duels.shared.game.team.GameTeam
import gg.minequest.duels.shared.game.team.GameTeamSide
import gg.minequest.duels.shared.ladder.DuelLadder
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
abstract class AbstractGame(
    val expectation: UUID,
    val teams: Map<GameTeamSide, GameTeam>,
    val ladder: DuelLadder
) : IDataStoreObject
{
    override val identifier: UUID
        get() = this.expectation

    var report: GameReport? = null
    var startTimestamp = -1L
}
