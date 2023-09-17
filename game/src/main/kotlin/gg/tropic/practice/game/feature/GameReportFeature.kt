package gg.tropic.practice.game.feature

import gg.tropic.practice.shared.game.GameReport
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.Lemon
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 1/2/2023
 */
@Service
object GameReportFeature
{
    private val connection = ScalaCommonsSpigot.instance.kvConnection
    private val matchPersistCacheMillis = TimeUnit.DAYS.toSeconds(3L)

    @Configure
    fun configure()
    {
        connection
    }

    fun saveSnapshotForAllParticipants(snapshot: GameReport): CompletableFuture<Void>
    {
        return CompletableFuture
            .runAsync {
                connection.sync().setex(
                    "duels:snapshots:matches:${snapshot.identifier}",
                    matchPersistCacheMillis,
                    Serializers.gson.toJson(snapshot)
                )
            }
            .thenRun {
                listOf(snapshot.winners, snapshot.losers)
                    .flatten()
                    .forEach {
                        connection.sync().setex(
                            "duels:snapshots:players:$it:matches:${snapshot.identifier}",
                            matchPersistCacheMillis, snapshot.identifier.toString()
                        )
                    }
            }
            .exceptionally {
                it.printStackTrace()
                return@exceptionally null
            }
    }
}
