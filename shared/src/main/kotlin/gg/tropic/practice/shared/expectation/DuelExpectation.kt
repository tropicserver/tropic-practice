package gg.tropic.practice.shared.expectation

import gg.tropic.practice.shared.game.team.GameTeam
import gg.tropic.practice.shared.game.team.GameTeamSide
import gg.tropic.practice.shared.ladder.DuelLadder
import gg.scala.store.storage.storable.IDataStoreObject
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
data class DuelExpectation(
    override val identifier: UUID,
    val players: List<UUID>,
    val teams: Map<GameTeamSide, GameTeam>,
    val ladder: DuelLadder
) : IDataStoreObject
