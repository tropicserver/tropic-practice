package gg.tropic.practice.metrics

import dev.cubxity.plugins.metrics.api.metric.collector.CollectorCollection
import gg.tropic.practice.metrics.collectors.PlayerDistributionCollector
import gg.tropic.practice.metrics.collectors.ReplicationCountCollector

/**
 * @author GrowlyX
 * @since 3/2/2024
 */
object GameServerCollection : CollectorCollection
{
    override val collectors = listOf(
        ReplicationCountCollector,
        PlayerDistributionCollector
    )

    override val isAsync = true
}
