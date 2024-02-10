package gg.tropic.practice.application.api.defaults.kit

import gg.tropic.practice.application.api.DPSDataSync
import gg.tropic.practice.application.api.DPSDataSyncKeys
import gg.tropic.practice.application.api.DPSDataSyncSource
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object KitDataSync : DPSDataSync<ImmutableKitContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-kits"

        override fun store() = Key.key(namespace(), "kits")
        override fun sync() = Key.key(namespace().suffixWhenDev(), "ksync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

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
