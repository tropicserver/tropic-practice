package gg.tropic.practice.integration

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.staff.reports.ReportProcessor
import gg.scala.staff.reports.ResponseAction

/**
 * @author GrowlyX
 * @since 1/16/2024
 */
@Service
@IgnoreAutoScan
@SoftDependency("ScStaff")
object StaffReportsService
{
    @Configure
    fun configure()
    {
        ReportProcessor.configureActionProvider { _, playerReport ->
            listOf(
                ResponseAction(
                    description = "Check the player's region",
                    command = "checkregion ${playerReport.to.username()}"
                ),
                ResponseAction(
                    description = "Check the player's ranked ban status",
                    command = "rankedbanstatus ${playerReport.to.username()}"
                ),
                ResponseAction(
                    description = "Spectate the player's game",
                    command = "spectate ${playerReport.to.username()}"
                ),
                ResponseAction(
                    description = "Terminate the player's game",
                    command = "terminatematch ${playerReport.to.username()}"
                ),
                ResponseAction(
                    description = "View information on the player's game",
                    command = "matchinfo ${playerReport.to.username()}"
                )
            )
        }
    }
}
