package gg.tropic.practice.hologram

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
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

    @Transient
    private var cache: LoadingCache<Pair<UUID, Reference>, Pair<Long?, Long?>>? = null
        get()
        {
            if (field == null)
            {
                field = CacheBuilder
                    .newBuilder()
                    .expireAfterWrite(1L, TimeUnit.MINUTES)
                    .build(object : CacheLoader<Pair<UUID, Reference>, Pair<Long?, Long?>>()
                    {
                        override fun load(key: Pair<UUID, Reference>) = LeaderboardManagerService
                            .getUserRankWithScore(key.first, key.second)
                            .join()
                    })
            }

            return field!!
        }

    internal var currentReference: Reference? = null
    abstract fun getNextReference(current: Reference?): Reference

    override fun getUpdateInterval() = 1000L
    override fun getNewLines(player: Player): List<String>
    {
        val lines = mutableListOf<String>()
        val currentRef = currentReference
            ?: return listOf(
                "${CC.GRAY}Loading..."
            )

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

        val cacheValue = cache!!.get(player.uniqueId to currentRef)
        lines += ""
        lines += "${CC.SEC}You: ${CC.PRI}${
            cacheValue.second
                ?.let(Numbers::format) ?: "???"
        } ${CC.GRAY}(#${
            cacheValue.first
                ?.let { it + 1 }
                ?.let(Numbers::format) ?: "???"
        })"
        lines += ""

        lines += LeaderboardManagerService
            .getCachedFormattedLeaderboards(currentRef)

        lines += ""
        lines += "${CC.SEC}Switches in ${CC.PRI}${
            TimeUtil.formatIntoMMSS(secondsUntilRefresh ?: 10)
        }${CC.SEC}..."

        return lines
    }

    internal fun invalidateCacheEntries(player: Player)
    {
        cache!!.asMap().filterKeys { it.first == player.uniqueId }
            .forEach { (key, _) ->
                cache!!.invalidate(key)
            }
    }
}
