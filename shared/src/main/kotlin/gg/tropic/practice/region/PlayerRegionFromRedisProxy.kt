package gg.tropic.practice.region

import net.evilblock.cubed.ScalaCommonsSpigot
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
object PlayerRegionFromRedisProxy
{
    fun of(player: Player) = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .hget("player:${player.uniqueId}", "instance")
        }
        .thenApply(Region::extractFrom)
}
