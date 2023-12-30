package gg.tropic.practice.hologram

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.services.LeaderboardManagerService
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.personalized.PersonalizedHologramEntity
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/30/2023
 */
abstract class AbstractScrollingLeaderboard(
    val scrollTime: Int,
    location: Location
) : AbstractTypeSerializable, PersonalizedHologramEntity(location)
{
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

    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1L, TimeUnit.MINUTES)
        .build(object : CacheLoader<Pair<UUID, Reference>, Pair<Long?, Long?>>()
        {
            override fun load(p0: Pair<UUID, Reference>) = LeaderboardManagerService
                .getUserRankWithScore(p0.first, p0.second)
                .join()
        })

    internal var currentReference: Reference? = null
    abstract fun getNextReference(current: Reference?): Reference

    override fun getUpdateInterval() = 1000L
    override fun getNewLines(player: Player): List<String>
    {
        val lines = mutableListOf<String>()
        val currentRef = currentReference
            ?: return lines

        lines += "${CC.PRI}${
            currentRef.kitID
                ?.let { id ->
                    KitService.cached()
                        .kits[id]?.displayName 
                }
                ?: "Global"
        }${CC.PRI} ${
            currentRef.leaderboardType.displayName
        }"

        val cacheValue = cache.get(player.uniqueId to currentRef)
        lines += ""
        lines += "${CC.SEC}You: ${CC.PRI}${
            cacheValue.first?.let(Numbers::format) ?: "???"
        } ${CC.GRAY}[#${
            cacheValue.second?.let(Numbers::format) ?: "???"
        }]"
        lines += ""

        lines += LeaderboardManagerService
            .getCachedFormattedLeaderboards(currentRef)

        lines += ""
        lines += "${CC.SEC}Switches in ${CC.PRI}${
            TimeUtil.formatIntoMMSS(secondsUntilRefresh!!)
        }${CC.SEC}..."

        return lines
    }

    internal fun invalidateCacheEntries(player: Player)
    {
        cache.asMap().filterKeys { it.first == player.uniqueId }
            .forEach { (key, _) ->
                cache.invalidate(key)
            }
    }
}
