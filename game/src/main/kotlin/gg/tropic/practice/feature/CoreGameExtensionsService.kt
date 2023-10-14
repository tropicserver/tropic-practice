package gg.tropic.practice.feature

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.game.extensions.cosmetics.CosmeticLocalConfig

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
@Service
@SoftDependency("CoreGameExtensions")
@IgnoreAutoScan
object CoreGameExtensionsService
{
    @Configure
    fun configure()
    {
        CosmeticLocalConfig.enableCosmeticResources = false
    }
}
