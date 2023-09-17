package gg.tropic.practice.games

import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
class GameReport(
    override val identifier: UUID,
    val winners: List<UUID>,
    val losers: List<UUID>,
    val snapshots: Map<UUID, GameReportSnapshot>,
    val duration: Long,
    val arena: String,
    val status: GameReportStatus,
    val matchDate: Date = Date(),
    var viewed: Boolean = false
) : IDataStoreObject
