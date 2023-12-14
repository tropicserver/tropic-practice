package gg.tropic.practice.application.api

import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 2/10/2023
 */
interface DPSDataSyncKeys
{
    fun store(): Key

    /**
     * It's incredible stupid that we're using
     * a [Key] for the store template.
     */
    fun newStore(): String
    {
        return store().asString()
    }
    fun sync(): Key

    fun keyOf(namespace: String, value: String) =
        Key.key(namespace, value)
}
