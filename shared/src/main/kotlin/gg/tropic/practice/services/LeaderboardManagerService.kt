package gg.tropic.practice.services

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.leaderboards.*
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
@Service
object LeaderboardManagerService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private val redis by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:leaderboards")
            .codec(AwareMessageCodec)
            .logger(plugin.logger)
            .build()
    }

    private var top10LeaderboardCache = mutableMapOf<Reference, List<LeaderboardEntry>>()
    private var references = LeaderboardReferences(listOf())

    fun getCachedLeaderboards(reference: Reference) = top10LeaderboardCache[reference]
        ?: listOf()

    fun updateScoreAndGetDiffs(user: UUID, reference: Reference, newScore: Long) =
        getUserRankWithScore(user, reference)
            .thenApplyAsync {
                ScalaCommonsSpigot.instance.kvConnection
                    .sync()
                    .zadd(
                        "tropicpractice:leaderboards:${reference.id()}:final",
                        newScore.toDouble(),
                        user.toString()
                    )
                it
            }
            .thenComposeAsync { old ->
                getUserRankWithScore(user, reference)
                    .thenApply { new ->
                        old to new
                    }
            }
            .thenApplyAsync {
                val nextPosition = it.second.first ?: 0
                val score = ScalaCommonsSpigot.instance.kvConnection.sync()
                    .zrangeWithScores(
                        "tropicpractice:leaderboards:${reference.id()}:final",
                        nextPosition, nextPosition
                    )
                    .firstOrNull()

                ScoreUpdates(
                    oldScore = it.first.second ?: 0,
                    oldPosition = nextPosition,
                    newPosition = nextPosition,
                    nextPosition = score?.run {
                        Position(
                            uniqueId = UUID.fromString(this.value),
                            score = this.score.toLong(),
                            position = nextPosition
                        )
                    }
                )
            }


    fun getUserRankWithScore(user: UUID, reference: Reference): CompletableFuture<Pair<Long?, Long?>> =
        CompletableFuture.supplyAsync {
            ScalaCommonsSpigot
                .instance.kvConnection.sync()
                .zrevrank(
                    "tropicpractice:leaderboards:${reference.id()}:final",
                    user.toString()
                )
        }.thenApplyAsync {
            if (it == null)
                return@thenApplyAsync null to null

            it to ScalaCommonsSpigot.instance.kvConnection.sync()
                .zscore("tropicpractice:leaderboards:${reference.id()}:final",
                    user.toString()).toLong()
        }

    fun rebuildLeaderboardCaches()
    {
        val references = runCatching {
            Serializers.gson.fromJson(
                ScalaCommonsSpigot.instance.kvConnection
                    .sync()
                    .get("tropicpractice:leaderboards:references"),
                LeaderboardReferences::class.java
            )
        }.getOrNull()
            ?: LeaderboardReferences(listOf())

        this.references = references

        val newLeaderboardCache = mutableMapOf<Reference, List<LeaderboardEntry>>()
        references.references.forEach {
            val scores = ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .zrevrangeWithScores(
                    "tropicpractice:leaderboards:${it.id()}:final",
                    0, 9
                )

            newLeaderboardCache[it] = scores
                .map { score ->
                    LeaderboardEntry(
                        uniqueId = UUID.fromString(score.value),
                        value = score.score.toLong()
                    )
                }
                .toList()
        }

        this.top10LeaderboardCache = newLeaderboardCache
        plugin.logger.info("[leaderboards] Rebuilt leaderboard caches at ${Instant.now()}.")
    }

    @Configure
    fun configure()
    {
        rebuildLeaderboardCaches()

        redis.listen("rebuild-cache") {
            rebuildLeaderboardCaches()
        }
        redis.connect().toCompletableFuture().join()
    }
}
