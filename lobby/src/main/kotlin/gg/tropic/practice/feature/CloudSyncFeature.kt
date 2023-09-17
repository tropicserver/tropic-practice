package gg.tropic.practice.feature

import gg.scala.cloudsync.shared.discovery.CloudSyncDiscoveryService
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@Service
@IgnoreAutoScan
@SoftDependency("cloudsync")
object CloudSyncFeature
{
    @Configure
    fun configure()
    {
        CloudSyncDiscoveryService
            .discoverable.assets.add(
                "gg.tropic.practice:game:TropicPractice-lobby${
                    if ("dev" in ServerSync.getLocalGameServer().groups) ":gradle-dev" else ""
                }"
            )
    }
}
