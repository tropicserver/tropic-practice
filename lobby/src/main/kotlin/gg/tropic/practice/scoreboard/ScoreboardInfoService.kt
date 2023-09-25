package gg.tropic.practice.scoreboard

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
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
        val online: Int,
        val playing: Int,
        val gameServers: Int,
        val queued: Int,
        val meanTPS: Double,
        val availableReplications: Int
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

    var scoreboardInfo = ScoreboardInfo(0, 0, 0, 0, 0.0, 0)

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                val gameServers = ServerContainer
                    .getServersInGroupCasted<GameServer>("mipgame")

                val servers = ServerContainer
                    .getServersInGroupCasted<GameServer>("mip")

                scoreboardInfo = ScoreboardInfo(
                    online = servers.sumOf { it.getPlayersCount() ?: 0 },
                    // TODO: load game impls and count from those
                    playing = gameServers.sumOf { it.getPlayersCount() ?: 0 },
                    gameServers = gameServers.size,
                    meanTPS = gameServers.map { it.getTPS()!! }.average(),
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
                        .count()
                )
            }, 5L, 5L)
    }
}
