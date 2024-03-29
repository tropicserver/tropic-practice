package gg.tropic.practice.region

import net.evilblock.cubed.ScalaCommonsSpigot
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
object PlayerRegionFromRedisProxy
{
    fun of(player: Player): CompletableFuture<Region> = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .hget("player:${player.uniqueId}", "instance")
        }
        .thenApply(Region::extractFrom)
        .exceptionally { Region.NA }

    fun ofPlayerID(player: UUID): CompletableFuture<Region> = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .hget("player:$player", "instance")
        }
        .thenApply(Region::extractFrom)
        .exceptionally { Region.NA }
}
