package gg.tropic.practice.metrics

import dev.cubxity.plugins.metrics.api.UnifiedMetrics
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.PracticeGame

/**
 * @author GrowlyX
 * @since 3/2/2024
 */
@Service
@IgnoreAutoScan
@SoftDependency("UnifiedMetrics")
object MetricsService
{
    @Inject
    lateinit var plugin: PracticeGame

    @Configure
    fun configure()
    {
        val provider = plugin.server.servicesManager
            .getRegistration(UnifiedMetrics::class.java)
            ?.provider
            ?: return

        provider.metricsManager.registerCollection(
            GameServerCollection
        )
    }
}
