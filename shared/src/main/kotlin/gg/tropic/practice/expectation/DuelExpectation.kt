package gg.tropic.practice.expectation

import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.ladder.DuelLadder
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
