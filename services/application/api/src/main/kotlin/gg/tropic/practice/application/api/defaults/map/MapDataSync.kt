package gg.tropic.practice.application.api.defaults.map

import gg.tropic.practice.application.api.DPSDataSync
import gg.tropic.practice.application.api.DPSDataSyncKeys
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object MapDataSync : DPSDataSync<ImmutableMapContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun store() = Key.key("tropicpractice", "maps")
        override fun sync() = Key.key("tropicpractice", "msync")
    }

    override fun keys() = DPSMapKeys
    override fun type() = ImmutableMapContainer::class.java
}
