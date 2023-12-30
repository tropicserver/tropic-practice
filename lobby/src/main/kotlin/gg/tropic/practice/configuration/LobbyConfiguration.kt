package gg.tropic.practice.configuration

import gg.tropic.practice.map.metadata.anonymous.Position
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
data class LobbyConfiguration(
    var spawnLocation: Position = Position(
        0.0, 100.0, 0.0, 180.0F, 0.0F
    ),
    val loginMOTD: MutableList<String> = mutableListOf(
        "",
        "${CC.B_PRI}Welcome to Tropic Practice ${CC.GRAY}(beta)",
        "${CC.GRAY}We are currently in BETA! Report bugs in our Discord.",
        ""
    ),
    var rankedQueueEnabled: Boolean = true,
    var rankedMinimumWinRequirement: Int? = 5
)
{
    fun minimumWinRequirement() = rankedMinimumWinRequirement ?: 5
}
