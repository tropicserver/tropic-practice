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
import java.util.LinkedList
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

    private var top10LeaderboardCache = mutableMapOf<Reference, LinkedList<LeaderboardEntry>>()

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

        val newLeaderboardCache = mutableMapOf<Reference, LinkedList<LeaderboardEntry>>()


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
