package gg.tropic.practice

import gg.scala.basics.plugin.settings.SettingContainer
import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.*
import gg.tropic.practice.reports.GameReportService
import gg.tropic.practice.provider.SettingProvider
import gg.tropic.practice.provider.impl.LemonSettingProvider
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
    name = "TropicPractice",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("Tropic")
@PluginWebsite("https://tropic.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("ScBasics"),
    PluginDependency("CoreGameExtensions", soft = true)
)
class PracticeLobby : ExtendedScalaPlugin()
{
    var settingProvider: SettingProvider = LemonSettingProvider

    init
    {
        PracticeShared
    }

    @ContainerEnable
    fun containerEnable()
    {
        SettingMenu.defaultCategory = "Practice"

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                GameReportService
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
