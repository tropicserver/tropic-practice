package gg.tropic.practice.replications.manager

import gg.tropic.practice.application.api.DPSRedisService

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object ReplicationManager
{
    private val redis = DPSRedisService("replicationmanager")
        .apply(DPSRedisService::start)

    fun load()
    {
        redis.configure {
            listen("status") {
                val server = retrieve<String>("server")
                // TODO: man what the fuck
            }
        }
    }
}
