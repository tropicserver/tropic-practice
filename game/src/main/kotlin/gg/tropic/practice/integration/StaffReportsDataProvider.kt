package gg.tropic.practice.integration

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.staff.reports.ReportProcessor
import gg.scala.staff.reports.ResponseAction
import gg.tropic.practice.games.GameService
import gg.tropic.practice.queue.QueueType
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit

/**
 * @author GrowlyX
 * @since 1/16/2024
 */
@Service
@IgnoreAutoScan
@SoftDependency("ScStaff")
object StaffReportsDataProvider
{
    @Configure
    fun configure()
    {
        ReportProcessor.provideAdditionalServerData { _, uuid ->
            val bukkitPlayer = Bukkit.getPlayer(uuid)
                ?: return@provideAdditionalServerData mapOf()

            val gameOfPlayer = GameService.byPlayer(bukkitPlayer)
                ?: return@provideAdditionalServerData mapOf()

            return@provideAdditionalServerData mapOf(
                "Kit" to gameOfPlayer.kit.displayName,
                "Ranked" to if (gameOfPlayer.expectationModel.queueType == QueueType.Ranked)
                    "${CC.GREEN}Yes" else "${CC.RED}No",
            )
        }
    }
}
