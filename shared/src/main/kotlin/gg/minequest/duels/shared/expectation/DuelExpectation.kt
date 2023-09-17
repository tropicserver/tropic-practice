package gg.minequest.duels.shared.expectation

import gg.minequest.duels.shared.game.team.GameTeam
import gg.minequest.duels.shared.game.team.GameTeamSide
import gg.minequest.duels.shared.ladder.DuelLadder
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
