package gg.tropic.practice.application.api.defaults.map

import gg.tropic.practice.application.api.DPSDataSync
import gg.tropic.practice.application.api.DPSDataSyncKeys
import gg.tropic.practice.application.api.DPSDataSyncSource
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.kit.group.ImmutableKitGroup
import gg.tropic.practice.application.api.defaults.kit.group.KitGroupDataSync
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object MapDataSync : DPSDataSync<ImmutableMapContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-maps"

        override fun store() = Key.key("tropicpractice", "maps")
        override fun sync() = Key.key("tropicpractice", "msync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = DPSMapKeys
    override fun type() = ImmutableMapContainer::class.java

    fun selectRandomMapCompatibleWith(kit: ImmutableKit): ImmutableMap?
    {
        val groups = KitGroupDataSync.groupsOf(kit)
            .map(ImmutableKitGroup::id)

        return cached().maps.values
            .filterNot(ImmutableMap::locked)
            .shuffled()
            .firstOrNull {
                groups.intersect(it.associatedKitGroups).isNotEmpty()
            }
    }
}
