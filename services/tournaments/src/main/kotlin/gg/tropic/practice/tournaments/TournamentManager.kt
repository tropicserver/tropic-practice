package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.serializable.Message
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
object TournamentManager : ScheduledExecutorService by Executors.newScheduledThreadPool(3)
{
    private val redis = DPSRedisService("tournaments")
        .apply(DPSRedisService::start)

    private var activeTournament: Tournament? = null
    private var tournamentCreationCooldown = -1L

    fun load()
    {
        redis.configure {
            listen("create") {
                val config = retrieve<TournamentConfig>("config")
                if (activeTournament != null)
                {
                    DPSRedisShared.sendMessage(
                        listOf(config.creator),
                        Message()
                            .withMessage("&cThere is already an active tournament!")
                    )
                    return@listen
                }

                val timeSinceLastCreation = System.currentTimeMillis() - tournamentCreationCooldown
                if (timeSinceLastCreation < Duration.ofMinutes(30L).toMillis())
                {
                    DPSRedisShared.sendMessage(
                        listOf(config.creator),
                        listOf(
                            "&cThere is a cooldown active for tournament creations!",
                            "&cPlease wait: &f${
                                DurationFormatUtils.formatDurationWords(
                                    Duration.ofMinutes(30L).toMillis() - timeSinceLastCreation,
                                    true, true
                                )
                            }"
                        )
                    )
                    return@listen
                }

                activeTournament = Tournament(config = config)
                activeTournament!!.startResources()

                DPSRedisShared.sendMessage(
                    listOf(config.creator),
                    Message()
                        .withMessage("&aYour tournament has been created!")
                )
            }
        }
    }
}
