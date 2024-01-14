package gg.tropic.practice

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.*
import gg.scala.lemon.channel.ChatChannelService
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.redirection.aggregate.impl.LeastTrafficServerAggregateHandler
import gg.tropic.practice.settings.ChatVisibility
import gg.tropic.practice.settings.DuelsSettingCategory
import org.bukkit.Bukkit

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
    PluginDependency("CoreGameExtensions"),
    PluginDependency("cloudsync", soft = true),
    PluginDependency("Apollo-Bukkit", soft = true),
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
                val chatVisibility = BasicsProfileService.find(other)
                    ?.setting(
                        "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:chat-visibility",
                        ChatVisibility.Global
                    )
                    ?: ChatVisibility.Global

                val bukkitPlayer = Bukkit.getPlayer(player)
                when (chatVisibility)
                {
                    ChatVisibility.Global -> true
                    ChatVisibility.Match -> bukkitPlayer != null && bukkitPlayer.world.name == other.world.name
                }
            }

        val lobbyRedirector = LeastTrafficServerAggregateHandler("miplobby")
        lobbyRedirector.subscribe()

        flavor {
            bind<ServerAggregateHandler>() to lobbyRedirector
        }
    }
}
