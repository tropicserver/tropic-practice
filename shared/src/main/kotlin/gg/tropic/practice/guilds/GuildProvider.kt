package gg.tropic.practice.guilds

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 2/3/2024
 */
interface GuildProvider
{
    fun provideGuildNameFor(uniqueId: UUID): CompletableFuture<String?>
}
