package gg.minequest.duels.external

import gg.minequest.duels.external.commands.DuelRequestsCommand
import gg.minequest.duels.external.feature.GameReportFeature
import gg.minequest.duels.external.provider.SettingProvider
import gg.minequest.duels.external.provider.impl.LemonSettingProvider
import gg.minequest.duels.shared.MinequestDuelsShared
import gg.minequest.duels.shared.game.GameReport
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.commands.ManualRegister
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.command.ScalaCommandManager
import gg.scala.commons.core.plugin.Plugin
import gg.scala.commons.core.plugin.PluginAuthor
import gg.scala.commons.core.plugin.PluginDependency
import gg.scala.commons.core.plugin.PluginDependencyComposite
import gg.scala.commons.core.plugin.PluginWebsite
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * @author GrowlyX
 * @since 8/5/2022
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
    PluginDependency("ScBasics", soft = true)
)
class MinequestDuelsExternal : ExtendedScalaPlugin()
{
    var settingProvider: SettingProvider = LemonSettingProvider

    @ManualRegister
    fun onManualRegister(manager: ScalaCommandManager)
    {
        if (server.pluginManager.isPluginEnabled("ScBasics"))
        {
            manager.registerCommand(DuelRequestsCommand)
        }
    }

    @ContainerEnable
    fun containerEnable()
    {
        MinequestDuelsShared.load()

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                GameReportFeature
                    .loadSnapshotsForParticipant(event.player.uniqueId)
                    .thenAcceptAsync {
                        val filtered = it
                            .filter { report ->
                                event.player.uniqueId in report.winners ||
                                        event.player.uniqueId in report.losers
                            }
                            .filter { report -> !report.viewed }

                        if (filtered.isNotEmpty())
                        {
                            FancyMessage()
                                .withMessage(
                                    "${CC.GREEN}Click this message to view your game overview!"
                                )
                                .andHoverOf("${CC.GREEN}Click to view!")
                                .andCommandOf(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/games"
                                )
                                .sendToPlayer(event.player)
                        }
                    }
            }
            .bindWith(this)
    }
}
