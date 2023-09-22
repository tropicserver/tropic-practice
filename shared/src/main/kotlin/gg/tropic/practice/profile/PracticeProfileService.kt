package gg.tropic.practice.profile

import gg.scala.commons.persist.ProfileOrchestrator
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.GameType
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

        GameType.entries
            .forEach {
                profile.casualStatistics.putIfAbsent(it, ConcurrentHashMap())
                profile.rankedStatistics.putIfAbsent(it, ConcurrentHashMap())
            }
    }
}
