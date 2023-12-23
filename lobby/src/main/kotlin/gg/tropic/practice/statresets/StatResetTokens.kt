package gg.tropic.practice.statresets

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
                    "tropicpractice:statreset-tokens:tokens",
                    user.toString()
                )
                ?.toIntOrNull()
        }
}
