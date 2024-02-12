package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.namespace
import gg.tropic.practice.serializable.Message
import gg.tropic.practice.suffixWhenDev
import io.netty.util.internal.ConcurrentSet
import net.evilblock.cubed.serializers.Serializers
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
object TournamentManager : ScheduledExecutorService by Executors.newScheduledThreadPool(3)
{
    val redis = DPSRedisService("tournaments")
        .apply(DPSRedisService::start)

    var activeTournament: Tournament? = null
    private var tournamentCreationCooldown = -1L

    fun load()
    {
        scheduleAtFixedRate({
            if (activeTournament == null)
            {
                return@scheduleAtFixedRate
            }

            DPSRedisShared.keyValueCache.sync()
                .setex(
                    "${namespace().suffixWhenDev()}:tournaments:members",
                    5L,
                    Serializers.gson.toJson(TournamentMemberList(
                        activeTournament!!.memberSet
                            .flatMap(TournamentMember::players)
                    ))
                )
        }, 0L, 100L, TimeUnit.MILLISECONDS)

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

                DPSRedisShared.sendMessage(
                    listOf(player),
                    Message()
                        .withMessage("&aEnded the active tournament!")
                )
            }

            listen("game-completion") {
                if (activeTournament == null)
                {
                    return@listen
                }

                val losers = retrieve<List<String>>("losers").toSet()
                activeTournament!!.currentRoundLosers += losers
                    .map {
                        UUID.fromString(it)
                    }
            }

            listen("force-start") {
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

                if (activeTournament!!.stateMachine.state != TournamentState.Populating)
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cThe tournament has already been started!")
                    )
                    return@listen
                }

                activeTournament!!.stateMachine.transition(StateEvent.OnPopulated)

                DPSRedisShared.sendMessage(
                    listOf(player),
                    Message()
                        .withMessage("&aForce started the tournament!")
                )
            }

            listen("leave") {
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

                if (!activeTournament!!.isInTournament(player))
                {
                    DPSRedisShared.sendMessage(
                        listOf(player),
                        Message()
                            .withMessage("&cYou are not in the tournament!")
                    )
                    return@listen
                }

                activeTournament!!.leaveTournament(player)

                DPSRedisShared.sendMessage(
                    listOf(player),
                    Message()
                        .withMessage("&cYou have left the tournament!")
                )
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

                tournamentCreationCooldown = System.currentTimeMillis()

                DPSRedisShared.sendMessage(
                    listOf(config.creator),
                    Message()
                        .withMessage("&aYour tournament has been created!")
                )
            }
        }
    }
}
