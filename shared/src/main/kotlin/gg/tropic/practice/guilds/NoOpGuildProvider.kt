package gg.tropic.practice.guilds

import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 2/3/2024
 */
object NoOpGuildProvider : GuildProvider
{
    override fun provideGuildNameFor(uniqueId: UUID): CompletableFuture<String?> =
        CompletableFuture.completedFuture(null)
}
