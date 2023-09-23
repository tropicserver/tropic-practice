package gg.tropic.practice.profile

import gg.scala.commons.persist.ProfileOrchestrator
import gg.scala.flavor.service.Service
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
@Service
object PracticeProfileService : ProfileOrchestrator<PracticeProfile>()
{
    override fun new(uniqueId: UUID) = PracticeProfile(uniqueId)
    override fun type() = PracticeProfile::class

    override fun postLoad(uniqueId: UUID)
    {
        val profile = find(uniqueId)
            ?: return

        KitService.cached().kits.values
            .forEach {
                profile.rankedStatistics.putIfAbsent(it.id, RankedKitStatistics())
                profile.casualStatistics.putIfAbsent(it.id, KitStatistics())

                profile.customLoadouts.putIfAbsent(it.id, ConcurrentHashMap())
            }
    }
}
