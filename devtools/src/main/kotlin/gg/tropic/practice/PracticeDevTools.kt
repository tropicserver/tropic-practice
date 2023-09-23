package gg.tropic.practice

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.core.plugin.*

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
@Plugin(
    name = "TropicPractice-DevTools",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("Tropic")
@PluginWebsite("https://tropic.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("ScBasics", soft = true)
)
class PracticeDevTools : ExtendedScalaPlugin()
{
    init
    {
        PracticeShared
    }
}
