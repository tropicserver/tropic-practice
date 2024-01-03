package gg.tropic.practice.autoscale

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Close
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapReplicationService
import gg.tropic.practice.map.MapService
import java.util.logging.Level

/**
 * @author GrowlyX
 * @since 1/1/2024
 */
@Service
@IgnoreAutoScan
object ReplicationAutoScaleTask : Thread("replication-auto-scale")
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private const val TARGET_REPLICATIONS = 16
    private const val FLOOR_REQUIRED_FOR_AUTO_SCALE = 0.45

    @Configure
    fun configure()
    {
        isDaemon = true
        start()
    }

    @Close
    fun close()
    {
        interrupt()
    }

    private fun runSilently()
    {
        if (!plugin.isEnabled)
        {
            return
        }

        val mappings = mutableMapOf<Map, Int>()
        for (map in MapService.maps())
        {
            val replications = MapReplicationService
                .findAllAvailableReplications(map)

            if (replications.size <= TARGET_REPLICATIONS * FLOOR_REQUIRED_FOR_AUTO_SCALE)
            {
                mappings[map] = TARGET_REPLICATIONS - replications.size
            }
        }

        if (mappings.isNotEmpty())
        {
            MapReplicationService
                .generateMapReplications(mappings)
                .thenAccept {
                    plugin.logger.info("Generated ${mappings.values.sum()} new map replications.")
                }
                .join()
        }
    }

    override fun run()
    {
        while (true)
        {
            runCatching(::runSilently)
                .onFailure {
                    plugin.logger.log(
                        Level.SEVERE,
                        "Failed to auto scale replications",
                        it
                    )
                }

            sleep(1000L)
        }
    }
}
