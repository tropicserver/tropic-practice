package gg.tropic.practice.kit

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeShared
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
@Service
object KitService : DataSyncService<KitContainer>()
{
    object KitKeys : DataSyncKeys
    {
        override fun store() = Key.key(PracticeShared.KEY, "kits")
        override fun sync() = Key.key(PracticeShared.KEY, "ksync")
    }

    override fun keys() = KitKeys
    override fun type() = KitContainer::class.java
}
