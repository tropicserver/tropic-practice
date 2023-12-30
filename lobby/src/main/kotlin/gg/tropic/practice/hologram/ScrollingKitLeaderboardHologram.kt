package gg.tropic.practice.hologram

import gg.scala.cache.uuid.ScalaStoreUuidCache
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
 * @since 6/30/2023
 */
class ScrollingKitLeaderboardHologram(
    private val scrollState: String,
    val scrollTime: Int,
    location: Location,
    val kits: List<String>
) : FormatUpdatingHologramEntity(
    "", location
)
{
    @Transient
    var state: String? = null
        get()
        {
            if (field == null)
            {
                field = kits.first()
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

    fun configure()
    {
        initializeData()
        EntityHandler.trackEntity(this)
    }

    override fun getTickInterval() = 20L
    override fun getNewLines(): List<String>
    {
        val lines = mutableListOf<String>()
        val kit = KitService.cached().kits[state]
            ?: return lines

        val leaderboardType = ReferenceLeaderboardType.valueOf(scrollState)
        lines.add(CC.PRI + kit.displayName + " " + leaderboardType.displayName)
        lines.add(CC.GRAY + "Switches in ${CC.WHITE}${
            TimeUtil.formatIntoMMSS(secondsUntilRefresh!!)
        }${CC.GRAY}...")
        lines.add(CC.GRAY)

        lines += LeaderboardManagerService.getCachedLeaderboards(
            Reference(leaderboardType = leaderboardType, kitID = state)
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
