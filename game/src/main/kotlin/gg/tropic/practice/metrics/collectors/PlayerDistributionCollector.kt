package gg.tropic.practice.metrics.collectors

import dev.cubxity.plugins.metrics.api.metric.collector.Collector
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric
import gg.tropic.practice.games.GameService

/**
 * @author GrowlyX
 * @since 3/2/2024
 */
object PlayerDistributionCollector : Collector
{
    override fun collect() = listOf(
        GaugeMetric(
            "practice_match_spectator_count", mapOf(),
            GameService.gameMappings.values.sumOf { it.expectedSpectators.size }
        )
    )
}
