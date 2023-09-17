package gg.minequest.duels.external.feature

import gg.minequest.duels.shared.game.GameReport
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
    val connection = ScalaCommonsSpigot.instance.kvConnection
    private val matchPersistCacheMillis = TimeUnit.DAYS.toSeconds(3L)

    @Configure
    fun configure()
    {
        connection
    }

    fun loadSnapshotsForParticipant(uniqueId: UUID): CompletableFuture<List<GameReport>>
    {
        return CompletableFuture
            .supplyAsync {
                connection.sync()
                    .keys("duels:snapshots:players:$uniqueId:matches:*")
            }
            .thenApply {
                it.map { key ->
                    // better than calling another GET
                    key.split(":")[5]
                }
            }
            .thenApply {
                it
                    .mapNotNull { uniqueId ->
                        connection.sync().get("duels:snapshots:matches:$uniqueId")
                    }
                    .mapNotNull {
                        kotlin
                            .runCatching {
                                Serializers.gson.fromJson(it, GameReport::class.java)
                            }
                            .getOrNull()
                    }
            }
            .exceptionally {
                it.printStackTrace()
                return@exceptionally listOf()
            }
    }
}
