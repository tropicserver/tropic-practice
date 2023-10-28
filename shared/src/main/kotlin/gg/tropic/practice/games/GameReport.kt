package gg.tropic.practice.games

import java.util.*

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
class GameReport(
    val identifier: UUID,
    val winners: List<UUID>,
    val losers: List<UUID>,
    val snapshots: Map<UUID, GameReportSnapshot>,
    val duration: Long,
    val map: String,
    val status: GameReportStatus,
    val matchDate: Date = Date()
)
