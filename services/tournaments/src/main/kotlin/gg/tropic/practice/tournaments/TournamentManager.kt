package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.serializable.Message
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.util.UUID
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
            listen("end") {
                val player = retrieve<UUID>("player")
                if (activeTournament == null)
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cThere is no active tournament!")
                    )
                    return@listen
                }

                activeTournament!!.stop()
            }

            listen("game-completion") {
                if (activeTournament == null)
                {
                    // never should happen
                    return@listen
                }

                val losers = retrieve<List<UUID>>("losers")

                activeTournament!!.memberSet.removeIf {
                    it.players.any { player -> player in losers }
                }
            }

            listen("join") {
                val player = retrieve<UUID>("player")
                val ableToBypassMaxSize = retrieve<Boolean>("canBypassMax")

                if (activeTournament == null)
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cThere is no active tournament!")
                    )
                    return@listen
                }

                if (activeTournament!!.isInTournament(player))
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cYou are already in the tournament!")
                    )
                    return@listen
                }

                if (activeTournament!!.stateMachine.state != TournamentState.Populating)
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cThe tournament has already been started!")
                    )
                    return@listen
                }

                val tournamentPlayers = activeTournament!!.memberSet
                    .flatMap(TournamentMember::players)
                    .size

                if (!ableToBypassMaxSize && tournamentPlayers >= activeTournament!!.config.maxPlayers)
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cThe tournament is already full!")
                    )
                    return@listen
                }

                activeTournament?.joinTournament(player)

                DPSRedisShared.sendMessage(
                    listOf(player),
                    Message()
                        .withMessage("&aYou have joined the tournament!")
                )
            }

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
