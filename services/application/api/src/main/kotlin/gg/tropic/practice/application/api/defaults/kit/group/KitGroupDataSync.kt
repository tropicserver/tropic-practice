package gg.tropic.practice.application.api.defaults.kit.group

import gg.tropic.practice.application.api.DPSDataSync
import gg.tropic.practice.application.api.DPSDataSyncKeys
import gg.tropic.practice.application.api.DPSDataSyncSource
import gg.tropic.practice.application.api.defaults.kit.ImmutableKit
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object KitGroupDataSync : DPSDataSync<ImmutableKitContainer>()
{
    object DPSKitGroupKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-kits-groups"

        override fun store() = Key.key("tropicpractice", "groups")
        override fun sync() = Key.key("tropicpractice", "gsync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = DPSKitGroupKeys
    override fun type() = ImmutableKitContainer::class.java

    fun groupsOf(kit: ImmutableKit) = cached().backingGroups
        .filter {
            kit.id in it.contains
        }
}
