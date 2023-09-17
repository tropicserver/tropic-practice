package gg.tropic.practice.profile

import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
data class PracticeProfile(
    override val identifier: UUID
) : IDataStoreObject
