package gg.tropic.practice.hologram

import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 6/30/2023
 */
class ScrollingKitLeaderboardHologram(
    private val leaderboardType: ReferenceLeaderboardType,
    private val kits: List<String>,
    scrollTime: Int, location: Location
) : AbstractScrollingLeaderboard(scrollTime, location)
{
    override fun getNextReference(current: Reference?) = Reference(
        leaderboardType = leaderboardType,
        kitID = current?.kitID
            ?.let {
                kits.getOrNull(
                    kits.indexOf(it) + 1
                )
            }
            ?: kits.first()
    )
}
