package gg.tropic.practice

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.*
import gg.scala.lemon.channel.ChatChannelService
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.redirection.aggregate.impl.LeastTrafficServerAggregateHandler

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Plugin(
    name = "TropicPractice",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("Tropic")
@PluginWebsite("https://tropic.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("SlimeWorldManager"),
    PluginDependency("cloudsync", soft = true)
)
class PracticeGame : ExtendedScalaPlugin()
{
    init
    {
        PracticeShared
    }

    @ContainerEnable
    fun containerEnable()
    {
        ChatChannelService.default
            .displayToPlayer { player, other ->
                player.world.name == other.world.name
            }

        val lobbyRedirector = LeastTrafficServerAggregateHandler("miplobby")
        lobbyRedirector.subscribe()

        flavor {
            bind<ServerAggregateHandler>() to lobbyRedirector
        }
    }
}
