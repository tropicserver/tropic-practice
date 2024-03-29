package gg.tropic.practice.scoreboard

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.agnostic.sync.server.region.Region
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.gameGroup
import gg.tropic.practice.namespace
import gg.tropic.practice.practiceGroup
import gg.tropic.practice.services.GameManagerService
import gg.tropic.practice.suffixWhenDev
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import kotlin.math.absoluteValue

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@Service
object ScoreboardInfoService
{
    data class ScoreboardInfo(
        val online: Int = 0,
        val playing: Int = 0,
        val gameServers: Int = 0,
        val queued: Int = 0,
        val meanTPS: Double = 0.0,
        val runningGames: Int = 0,
        val percentagePlaying: Float = 0.0F,
        val availableReplications: Int = 0,
        val euServerTotalPlayers: Int = 0,
        val naServerTotalPlayers: Int = 0
    )

    data class ReplicationIndexes(
        val indexes: Map<String, String>
    )

    data class Replication(
        val server: String,
        val associatedMapName: String,
        val name: String,
        val inUse: Boolean = false
    )

    data class ReplicationStatus(
        val replications: Map<String, List<Replication>>
    )

    var scoreboardInfo = ScoreboardInfo()

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                runCatching {
                    val gameServers = ServerContainer
                        .getServersInGroupCasted<GameServer>(gameGroup().suffixWhenDev())

                    val servers = ServerContainer
                        .getServersInGroupCasted<GameServer>(practiceGroup().suffixWhenDev())

                    val naServersTotalPlayerCount = servers
                        .filter { it.region == Region.NA }
                        .sumOf { it.getPlayersCount()!! }

                    val euServersTotalPlayerCount = servers
                        .filter { it.region == Region.EU }
                        .sumOf { it.getPlayersCount()!! }

                    val games = GameManagerService.allGames().join()

                    val online = servers.sumOf { it.getPlayersCount() ?: 0 }
                    val playing = gameServers.sumOf { it.getPlayersCount() ?: 0 }
                    val percentage = (if (online == 0) 0.0F else (playing.toFloat() / online.toFloat())) * 100

                    scoreboardInfo = ScoreboardInfo(
                        online = servers.sumOf { it.getPlayersCount() ?: 0 },
                        playing = gameServers.sumOf { it.getPlayersCount() ?: 0 },
                        gameServers = gameServers.size,
                        meanTPS = gameServers.map { it.getTPS()!! }.average(),
                        runningGames = games.count(),
                        percentagePlaying = percentage,
                        queued = ScalaCommonsSpigot
                            .instance
                            .kvConnection
                            .sync()
                            .hlen("${namespace().suffixWhenDev()}:queue-states")
                            .toInt(),
                        availableReplications = ScalaCommonsSpigot
                            .instance
                            .kvConnection
                            .sync()
                            .get("${namespace().suffixWhenDev()}:replicationmanager:status-indexes")
                            .let {
                                Serializers.gson.fromJson(
                                    it, ReplicationIndexes::class.java
                                )
                            }
                            .indexes
                            .mapValues {
                                Serializers.gson.fromJson(
                                    ScalaCommonsSpigot
                                        .instance
                                        .kvConnection
                                        .sync()
                                        .get(it.value),
                                    ReplicationStatus::class.java
                                )
                            }
                            .flatMap {
                                kotlin
                                    .runCatching {
                                        // replicationmanager could be null?
                                        it.value.replications.values.flatten()
                                    }
                                    .getOrNull()
                                    ?: listOf()
                            }
                            .count(),
                        euServerTotalPlayers = euServersTotalPlayerCount,
                        naServerTotalPlayers = naServersTotalPlayerCount
                    )
                }
            }, 5L, 5L)
    }
}
