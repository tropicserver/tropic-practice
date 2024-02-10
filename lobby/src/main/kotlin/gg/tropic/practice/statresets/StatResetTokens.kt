package gg.tropic.practice.statresets

import gg.tropic.practice.namespace
import net.evilblock.cubed.ScalaCommonsSpigot
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 7/12/2023
 */
object StatResetTokens
{
    fun of(user: UUID) = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .hget(
                    "${namespace()}:statreset-tokens:tokens",
                    user.toString()
                )
                ?.toIntOrNull()
        }
}
