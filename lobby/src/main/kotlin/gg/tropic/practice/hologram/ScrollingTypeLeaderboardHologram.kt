package gg.tropic.practice.hologram

import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 12/29/2023
 */
class ScrollingTypeLeaderboardHologram(
    private val kit: String?,
    private val leaderboardTypes: List<ReferenceLeaderboardType>,
    scrollTime: Int, location: Location
) : AbstractScrollingLeaderboard(scrollTime, location)
{
    override fun getNextReference(current: Reference?) = Reference(
        leaderboardType = current?.leaderboardType
            ?.let {
                leaderboardTypes.getOrNull(
                    leaderboardTypes.indexOf(it) + 1
                )
            }
            ?: leaderboardTypes.first(),
        kitID = kit
    )

    override fun getAbstractType() = ScrollingTypeLeaderboardHologram::class.java
}
