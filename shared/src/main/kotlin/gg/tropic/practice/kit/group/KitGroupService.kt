package gg.tropic.practice.kit.group

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeShared
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
@Service
object KitGroupService : DataSyncService<KitGroupContainer>()
{
    object GroupKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-kits-groups"

        override fun store() = Key.key(namespace(), "groups")
        override fun sync() = Key.key(namespace().suffixWhenDev(), "gsync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = GroupKeys
    override fun type() = KitGroupContainer::class.java

    fun groupsOf(kit: Kit) = cached().groups
        .filter {
            kit.id in it.contains
        }
}
