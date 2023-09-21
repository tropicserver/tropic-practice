package gg.tropic.practice.profile

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.games.GameType
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
    val statistics = mutableMapOf<
        GameType,
        ConcurrentHashMap<
            String,
            Int
        >
    >(

    )
}
