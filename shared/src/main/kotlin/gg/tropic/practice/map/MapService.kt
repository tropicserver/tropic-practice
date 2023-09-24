package gg.tropic.practice.map

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeShared
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
@Service
object MapService : DataSyncService<MapContainer>()
{
    object MapKeys : DataSyncKeys
    {
        override fun store() = Key.key(PracticeShared.KEY, "maps")
        override fun sync() = Key.key(PracticeShared.KEY, "msync")
    }

    override fun keys() = MapKeys
    override fun type() = MapContainer::class.java

    fun maps() = cached().maps.values

    fun mapWithID(id: String) = cached().maps.values
        .firstOrNull {
            it.name.equals(id, true)
        }

    // TODO: Are we even going to do this? Can we just forward it to the
    //  queue system to allocate and do its shit? it's just easier for duels
    fun selectRandomMapCompatibleWith(kit: Kit): Map?
    {
        val groups = KitGroupService.groupsOf(kit)
            .map(KitGroup::id)

        return cached().maps.values
            .shuffled()
            .firstOrNull {
                groups.intersect(it.associatedKitGroups).isNotEmpty()
            }
    }
}
