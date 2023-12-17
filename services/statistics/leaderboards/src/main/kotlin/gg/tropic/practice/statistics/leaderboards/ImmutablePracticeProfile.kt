package gg.tropic.practice.statistics.leaderboards

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.statistics.GlobalStatistics
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import java.util.*

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
data class ImmutablePracticeProfile(
    override val identifier: UUID
) : IDataStoreObject
{
    val globalStatistics: GlobalStatistics = GlobalStatistics()

    val casualStatistics: Map<String, KitStatistics> = mapOf()
    val rankedStatistics: Map<String, RankedKitStatistics> = mapOf()
}
