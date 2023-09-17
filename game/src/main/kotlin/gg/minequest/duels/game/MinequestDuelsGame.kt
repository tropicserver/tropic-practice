package gg.minequest.duels.game

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.Plugin
import gg.scala.commons.core.plugin.PluginAuthor
import gg.scala.commons.core.plugin.PluginDependency
import gg.scala.commons.core.plugin.PluginDependencyComposite
import gg.scala.commons.core.plugin.PluginWebsite
import gg.scala.lemon.channel.ChatChannelService
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.redirection.aggregate.impl.LeastTrafficServerAggregateHandler
import net.kyori.adventure.platform.bukkit.BukkitAudiences

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Plugin(
    name = "MinequestDuels",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("Scala")
@PluginWebsite("https://scala.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("SlimeWorldManager"),
    PluginDependency("cloudsync", soft = true)
)
class MinequestDuelsGame : ExtendedScalaPlugin()
{
    private lateinit var lobbyRedirector: ServerAggregateHandler

    @ContainerEnable
    fun containerEnable()
    {
        this.lobbyRedirector = LeastTrafficServerAggregateHandler("hub")
        this.lobbyRedirector.subscribe()

        ChatChannelService.default
            .displayToPlayer { player, other ->
                player.world.name == other.world.name
            }

        flavor {
            bind<ServerAggregateHandler>() to this@MinequestDuelsGame.lobbyRedirector

            bind<BukkitAudiences>() to BukkitAudiences
                .create(this@MinequestDuelsGame)
        }
    }
}
