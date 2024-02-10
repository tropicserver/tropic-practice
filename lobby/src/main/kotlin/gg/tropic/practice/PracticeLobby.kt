package gg.tropic.practice

import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.*
import gg.tropic.practice.provider.SettingProvider
import gg.tropic.practice.provider.impl.LemonSettingProvider
import gg.tropic.practice.services.GameManagerService

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
    PluginDependency("Parties"),
    PluginDependency("ScStaff", soft = true),
    PluginDependency("Friends", soft = true),
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
        devProvider = {
            "miplobbyDEV" in ServerSync.getLocalGameServer().groups
        }

        SettingMenu.defaultCategory = "Practice"
        GameManagerService.bindToMetadataService()
    }
}
