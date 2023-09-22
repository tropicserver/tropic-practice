package gg.tropic.practice.profile

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.games.GameType
import gg.tropic.practice.statistics.GlobalStatistics
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
data class PracticeProfile(
    override val identifier: UUID
) : IDataStoreObject
{
    val globalStatistics = GlobalStatistics()
    val casualStatistics = mutableMapOf<
        GameType,
        ConcurrentHashMap<
            String,
            KitStatistics
        >
    >()

    val rankedStatistics = mutableMapOf<
        GameType,
        ConcurrentHashMap<
            String,
            RankedKitStatistics
        >
    >()
}
