package gg.tropic.practice.replications.manager

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import gg.scala.aware.thread.AwareThreadContext
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import net.evilblock.cubed.serializers.Serializers
import java.util.*
import java.util.concurrent.CompletableFuture
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

    private val gameInstanceCache = Caffeine.newBuilder()
        .expireAfterWrite(2L, TimeUnit.SECONDS)
        .removalListener<String, String> { _, _, _ -> syncStatusIndexes() }
        .build<String, String>()

    private val dpsCache = DPSRedisShared.keyValueCache

    data class AllServerStatuses(
        val indexes: Map<String, String>
    )

    fun allServerStatuses() = dpsCache
        .sync()
        .get("tropicpractice:replicationmanager:status-indexes")
        .let {
            Serializers.gson.fromJson(
                it, AllServerStatuses::class.java
            )
        }
        .indexes.mapValues { (_, value) ->
            dpsCache
                .sync()
                .get(value)
                .let {
                    Serializers.gson.fromJson(
                        it, ReplicationStatus::class.java
                    )
                }
        }

    private fun syncStatusIndexes()
    {
        ForkJoinPool.commonPool().submit {
            dpsCache.sync().set(
                "tropicpractice:replicationmanager:status-indexes",
                Serializers.gson.toJson(
                    AllServerStatuses(gameInstanceCache.asMap())
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

            listen("replication-ready") {
                val requestID = retrieve<UUID>("requestID")
                val result = retrieve<ReplicationResult>("result")

                replicationCallbacks.getIfPresent(requestID)
                    ?.complete(result)
            }
        }
    }

    enum class ReplicationResultStatus
    {
        Completed,
        Unavailable
    }

    data class ReplicationResult(
        val status: ReplicationResultStatus,
        val message: String? = null
    )

    private val replicationCallbacks = Caffeine
        .newBuilder()
        .removalListener<UUID, CompletableFuture<ReplicationResult>> { _, value, cause ->
            if (cause == RemovalCause.EXPIRED)
            {
                value?.complete(
                    ReplicationResult(
                        status = ReplicationResultStatus.Unavailable,
                        message = "We weren't able to allocate a map for you!"
                    )
                )
            }
        }
        .expireAfterWrite(5L, TimeUnit.SECONDS)
        .build<UUID, CompletableFuture<ReplicationResult>>()

    fun allocateReplication(
        server: String,
        map: String,
        expectationID: UUID
    ): CompletableFuture<ReplicationResult>
    {
        val future = CompletableFuture<ReplicationResult>()
        val requestID = UUID.randomUUID()
        redis.createMessage(
            "allocate-replication",
            "requestID" to requestID,
            "expectationID" to expectationID,
            "map" to map,
            "server" to server
        ).publish(
            AwareThreadContext.SYNC
        )

        replicationCallbacks.put(requestID, future)
        return future
    }

    fun requestReplication(server: String, map: String, expectationID: UUID): CompletableFuture<ReplicationResult>
    {
        val future = CompletableFuture<ReplicationResult>()
        val requestID = UUID.randomUUID()
        redis.createMessage(
            "request-replication",
            "requestID" to requestID,
            "expectationID" to expectationID,
            "map" to map,
            "server" to server
        ).publish(
            AwareThreadContext.SYNC
        )

        replicationCallbacks.put(requestID, future)
        return future
    }
}
