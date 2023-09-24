package gg.tropic.practice.application.api.defaults.kit

import gg.tropic.practice.application.api.DPSDataSync
import gg.tropic.practice.application.api.DPSDataSyncKeys
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object KitDataSync : DPSDataSync<ImmutableKitContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun store() = Key.key("tropicpractice", "kits")
        override fun sync() = Key.key("tropicpractice", "ksync")
    }

    override fun keys() = DPSMapKeys
    override fun type() = ImmutableKitContainer::class.java

    private val hooks = mutableListOf<() -> Unit>()

    fun onReload(hook: () -> Unit) = hooks.add(hook)

    override fun postReload()
    {
        super.postReload()
        hooks.forEach { it() }
    }
}
