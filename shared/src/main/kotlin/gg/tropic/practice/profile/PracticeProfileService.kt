package gg.tropic.practice.profile

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.persist.ProfileOrchestrator
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import gg.tropic.practice.suffixWhenDev
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
@Service
object PracticeProfileService : ProfileOrchestrator<PracticeProfile>()
{
    override fun new(uniqueId: UUID) = PracticeProfile(uniqueId)
    override fun type() = PracticeProfile::class

    @Configure
    fun configure()
    {
        aware.listen("propagate") {
            val uniqueId = retrieve<UUID>("player")
            Bukkit.getPlayer(uniqueId) ?: return@listen

            with(
                DataStoreObjectControllerCache
                    .findNotNull<PracticeProfile>()
            ) {
                load(
                    uniqueId,
                    DataStoreStorageType.MONGO
                ).thenAcceptAsync {
                    if (it != null)
                    {
                        localCache().computeIfPresent(uniqueId) { _, _ -> it }
                    }
                }
            }
        }
        aware.connect().toCompletableFuture().join()
    }

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:profiles".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    fun sendMessage(packet: String, vararg pairs: Pair<String, Any?>) =
        CompletableFuture.runAsync {
            AwareMessage.of(packet, this.aware, *pairs)
                .publish(AwareThreadContext.SYNC)
        }

    override fun postLoad(uniqueId: UUID)
    {
        val profile = find(uniqueId)
            ?: return

        var hasAnyUpdates = false

        KitService.cached().kits.values
            .forEach {
                val requiresUpdates = listOf(
                    profile.rankedStatistics.putIfAbsent(it.id, RankedKitStatistics()),
                    profile.casualStatistics.putIfAbsent(it.id, KitStatistics()),
                    profile.customLoadouts.putIfAbsent(it.id, mutableListOf())
                ).any { update -> update == null }

                if (requiresUpdates)
                {
                    hasAnyUpdates = true
                }
            }

        if (hasAnyUpdates)
        {
            profile.save()
        }
    }
}
