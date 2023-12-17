package gg.tropic.practice.services

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.leaderboards.LeaderboardEntry
import gg.tropic.practice.leaderboards.LeaderboardReferences
import gg.tropic.practice.leaderboards.Reference
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

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

    fun getUserRankIn(user: UUID, reference: Reference): CompletableFuture<Long?> =
        CompletableFuture.supplyAsync {
            ScalaCommonsSpigot
                .instance.kvConnection.sync()
                .zrevrank(
                    "tropicpractice:leaderboards:${reference.id()}:final",
                    user.toString()
                )
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
