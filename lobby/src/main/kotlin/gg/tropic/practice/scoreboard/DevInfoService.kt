package gg.tropic.practice.scoreboard

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@Service
object DevInfoService
{
    @Inject
    lateinit var plugin: PracticeLobby

    data class DevInfo(
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

    var devInfo = DevInfo(0, 0, 0.0, 0)

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                val servers = ServerContainer
                    .getServersInGroupCasted<GameServer>("mipgame")

                devInfo = DevInfo(
                    gameServers = servers.size,
                    meanTPS = servers.map { it.getTPS()!! }.average(),
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
                            it.value.replications.values.flatten()
                        }
                        .count()
                )
            }, 5L, 5L)
    }
}
