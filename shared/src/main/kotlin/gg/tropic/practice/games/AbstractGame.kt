package gg.tropic.practice.games

import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.kit.Kit
import java.util.*

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
abstract class AbstractGame(
    val expectationModel: GameExpectation,
    val teams: Map<GameTeamSide, GameTeam>,
    val kit: Kit,
    val expectation: UUID = expectationModel.identifier
)
{
    val identifier: UUID
        get() = this.expectation

    var report: GameReport? = null
    var startTimestamp = -1L
}
