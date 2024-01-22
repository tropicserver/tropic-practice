package gg.tropic.practice.configuration

import gg.tropic.practice.configuration.subconfig.DataSampleThresholds
import gg.tropic.practice.map.metadata.anonymous.Position
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
data class PracticeConfiguration(
    var spawnLocation: Position = Position(
        0.0, 100.0, 0.0, 180.0F, 0.0F
    ),
    val loginMOTD: MutableList<String> = mutableListOf(
        "",
        "${CC.B_PRI}Welcome to Tropic Practice",
        "${CC.GRAY}We are currently in BETA! Report bugs in our Discord.",
        ""
    ),
    var rankedQueueEnabled: Boolean = true,
    var enableMIPTabHandler: Boolean? = true,
    var rankedMinimumWinRequirement: Int? = 5,
    var sampleThresholds: DataSampleThresholds? = DataSampleThresholds()
)
{
    fun dataSampleThresholds(): DataSampleThresholds
    {
        if (sampleThresholds == null)
        {
            sampleThresholds = DataSampleThresholds()
        }

        return sampleThresholds!!
    }

    fun enableMIPTabHandler() = enableMIPTabHandler ?: true
    fun minimumWinRequirement() = rankedMinimumWinRequirement ?: 5
}
