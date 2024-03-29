package gg.tropic.practice.statistics.leaderboards

import gg.scala.aware.thread.AwareThreadContext
import gg.scala.store.ScalaDataStoreShared
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.leaderboards.LeaderboardReferences
import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import io.lettuce.core.LettuceFutures
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
object LeaderboardManager : () -> Unit
{
    private val redis = DPSRedisService("leaderboards")
        .apply(DPSRedisService::start)

    private val connection = redis.configure { internal().connect() }

    private val practiceProfileCollection = ScalaDataStoreShared.INSTANCE.getNewMongoConnection()
        .getAppliedResource()
        .getCollection("PracticeProfile")

    private var thread: Thread? = null
    private var leaderboards = mutableListOf<Leaderboard>()
    private var initial = true

    private fun rebuildLeaderboardIndexes()
    {
        val leaderboards = mutableListOf<Leaderboard>()
        LeaderboardType.entries.forEach {
            for (kit in KitDataSync.cached().kits.values)
            {
                if (
                    it.enforceRanked &&
                    !kit.features(FeatureFlag.Ranked)
                )
                {
                    continue
                }

                leaderboards += Leaderboard(
                    leaderboardType = it,
                    kit = kit,
                    initial = initial
                )
            }

            leaderboards += Leaderboard(
                leaderboardType = it,
                kit = null,
                initial = initial
            )
        }

        this.leaderboards = leaderboards
        this.initial = false
    }

    fun load()
    {
        check(thread == null)
        connection.setAutoFlushCommands(false)

        rebuildLeaderboardIndexes()

        thread = thread(
            name = "leaderboard-updater",
            block = {
                while (true)
                {
                    runCatching(::invoke).onFailure {
                        Logger.getAnonymousLogger().log(
                            Level.SEVERE, "Failed to update leaderboards", it
                        )
                    }

                    redis.createMessage("rebuild-cache")
                        .publish(AwareThreadContext.SYNC)

                    runCatching {
                        Thread.sleep(15 * 1000L)
                    }.onFailure(Throwable::printStackTrace)
                }
            }
        )
    }

    override fun invoke()
    {
        val start = System.currentTimeMillis()
        val futures = leaderboards.map {
            connection.async()
                .del(
                    "${namespace().suffixWhenDev()}:leaderboards:${it.leaderboardId()}:staging"
                )
        }

        rebuildLeaderboardIndexes()

        val updatedReferences = Serializers.gson.toJson(
            LeaderboardReferences(
                references = leaderboards
                    .map {
                        Reference(
                            leaderboardType = ReferenceLeaderboardType
                                .valueOf(it.leaderboardType.name),
                            kitID = it.kit?.id
                        )
                    }
            )
        )

        connection.async().set(
            "${namespace().suffixWhenDev()}:leaderboards:references",
            updatedReferences
        )

        connection.flushCommands()
        LettuceFutures.awaitAll(
            5, TimeUnit.SECONDS,
            *futures.toTypedArray()
        )

        Logger.getGlobal()
            .info(
                "[leaderboards] removed staging leaderboards in ${System.currentTimeMillis() - start}ms"
            )

        practiceProfileCollection.find().forEach { document ->
            val profile = Serializers.gson.fromJson(
                document.toJson(),
                ImmutablePracticeProfile::class.java
            )

            leaderboards.forEach {
                val value = kotlin.runCatching {
                    if (it.kit == null)
                    {
                        it.leaderboardType.fromGlobal(profile)
                    } else
                    {
                        it.leaderboardType.fromKit(profile, it.kit)
                    }
                }.onFailure { throwable ->
                    Logger.getGlobal().log(
                        Level.SEVERE,
                        "Couldn't update player ${profile.identifier} for ${it.leaderboardId()}",
                        throwable
                    )
                }.getOrNull()

                if (value != null)
                {
                    connection.async().zadd(
                        "${namespace().suffixWhenDev()}:leaderboards:${it.leaderboardId()}:staging",
                        value.toDouble(),
                        profile.identifier.toString()
                    )
                }
            }

            connection.flushCommands()
        }

        val renameFutures = leaderboards.map {
            connection.async().rename(
                "${namespace().suffixWhenDev()}:leaderboards:${it.leaderboardId()}:staging",
                "${namespace().suffixWhenDev()}:leaderboards:${it.leaderboardId()}:final"
            )
        }

        connection.flushCommands()

        LettuceFutures.awaitAll(
            30, TimeUnit.SECONDS,
            *renameFutures.toTypedArray()
        )

        Logger.getGlobal()
            .info(
                "[leaderboards] finished updating leaderboards in ${System.currentTimeMillis() - start}ms"
            )
    }
}
