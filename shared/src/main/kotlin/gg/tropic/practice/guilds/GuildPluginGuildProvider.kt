package gg.tropic.practice.guilds

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.game.extensions.guilds.GuildService
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 2/3/2024
 */
@Service
@IgnoreAutoScan
@SoftDependency("CoreGameExtensions")
object GuildPluginGuildProvider : GuildProvider
{
    @Configure
    fun configure()
    {
        Guilds.guildProvider = this
    }

    override fun provideGuildNameFor(uniqueId: UUID) = GuildService
        .guildByUser(uniqueId)
        .thenApplyAsync { it?.name }
}
