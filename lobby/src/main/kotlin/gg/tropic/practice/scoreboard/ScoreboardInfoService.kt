package gg.tropic.practice.scoreboard

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.agnostic.sync.server.region.Region
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.services.GameManagerService
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers

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
                        .getServersInGroupCasted<GameServer>("mipgame")

                    val servers = ServerContainer
                        .getServersInGroupCasted<GameServer>("mip")

                    val naServersTotalPlayerCount = servers
                        .filter { it.region == Region.NA }
                        .sumOf { it.getPlayersCount()!! }

                    val euServersTotalPlayerCount = servers
                        .filter { it.region == Region.EU }
                        .sumOf { it.getPlayersCount()!! }

                    val games = GameManagerService.allGames().join()

                    scoreboardInfo = ScoreboardInfo(
                        online = servers.sumOf { it.getPlayersCount() ?: 0 },
                        // TODO: load game impls and count from those
                        playing = gameServers.sumOf { it.getPlayersCount() ?: 0 },
                        gameServers = gameServers.size,
                        meanTPS = gameServers.map { it.getTPS()!! }.average(),
                        runningGames = games.count(),
                        queued = ScalaCommonsSpigot
                            .instance
                            .kvConnection
                            .sync()
                            .hlen("tropicpractice:queue-states")
                            .toInt(),
                        availableReplications = ScalaCommonsSpigot
                            .instance
                            .kvConnection
                            .sync()
                            .get("tropicpractice:replicationmanager:status-indexes")
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
