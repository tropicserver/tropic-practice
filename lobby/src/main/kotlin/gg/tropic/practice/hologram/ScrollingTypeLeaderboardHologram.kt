package gg.tropic.practice.hologram

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import gg.tropic.practice.services.LeaderboardManagerService
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.updating.FormatUpdatingHologramEntity
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 12/29/2023
 */
class ScrollingTypeLeaderboardHologram(
    val scrollStates: List<String>,
    val scrollTime: Int,
    location: Location,
    private val kitID: String? = null
) : FormatUpdatingHologramEntity(
    "", location
)
{
    @Transient
    var state: ReferenceLeaderboardType? = null
        get()
        {
            if (field == null)
            {
                field = ReferenceLeaderboardType
                    .valueOf(scrollStates.first())
            }

            return field!!
        }

    @Transient
    var secondsUntilRefresh: Int? = null
        get()
        {
            if (field == null)
            {
                field = scrollTime
            }

            return field!!
        }

    private val kit: Kit?
        get()
        {
            return KitService.cached().kits[kitID]
        }

    fun configure()
    {
        initializeData()
        EntityHandler.trackEntity(this)
    }

    override fun getTickInterval() = 20L
    override fun getNewLines(): List<String>
    {
        val lines = mutableListOf<String>()

        lines.add(CC.PRI + (kit?.displayName ?: "Global") + " ${state!!.displayName}")
        lines.add(CC.GRAY + "Switches in ${CC.WHITE}${
            TimeUtil.formatIntoMMSS(secondsUntilRefresh!!)
        }${CC.GRAY}...")
        lines.add(CC.GRAY)

        lines += LeaderboardManagerService.getCachedLeaderboards(
            Reference(leaderboardType = state!!, kitID = kitID)
        ).mapIndexed { index, entry ->
            "${CC.PRI}#${index + 1}. ${CC.WHITE}${
                ScalaStoreUuidCache.username(entry.uniqueId)
            } ${CC.GRAY}- ${CC.PRI}${
                Numbers.format(entry.value)
            }"
        }

        return lines
    }
}
