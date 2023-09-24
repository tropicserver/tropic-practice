package gg.tropic.practice.replications.manager

import com.google.common.cache.CacheBuilder
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object ReplicationManager
{
    private val redis = DPSRedisService("replicationmanager")
        .apply(DPSRedisService::start)

    private val gameInstanceCache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(2L, TimeUnit.SECONDS)
        .removalListener<String, String> {
            syncStatusIndexes()
        }
        .build<String, String>()

    private val dpsCache = DPSRedisShared.keyValueCache

    private fun syncStatusIndexes()
    {
        ForkJoinPool.commonPool().submit {
            dpsCache.sync().set(
                "tropicpractice:replicationmanager:status-indexes",
                Serializers.gson.toJson(
                    gameInstanceCache.asMap()
                )
            )
        }
    }

    fun load()
    {
        redis.configure {
            listen("status") {
                val server = retrieve<String>("server")
                val status = retrieve<ReplicationStatus>("status")

                val key = "tropicpractice:replicationmanager:status:$server"

                dpsCache.sync().psetex(
                    key, 1000 * 2,
                    Serializers.gson.toJson(status)
                )

                gameInstanceCache.put(server, key)
                syncStatusIndexes()
            }
        }
    }
}
