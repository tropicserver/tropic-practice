package gg.tropic.practice.tournaments

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.game.GameTeam
import gg.tropic.practice.application.api.defaults.game.GameTeamSide
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.queue.GameQueueManager
import gg.tropic.practice.serializable.Message
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
sealed class TournamentState
{
    data object Populating : TournamentState()
    data object RoundStarting : TournamentState()
    data object RoundInProgress : TournamentState()
    data object Ended : TournamentState()
}

sealed class StateEvent
{
    data object OnPopulated : StateEvent()
    data object OnRoundsStarted : StateEvent()
    data object OnRoundsEnded : StateEvent()
}

sealed class SideEffect : (Tournament) -> Unit
{
    data object DetermineNextMatchList : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {
            val chunked = tournament.memberSet.shuffled()
                .chunked(2)
                .toMutableList()
            var strayComponent = chunked.last()

            if (strayComponent.size < 2)
            {
                chunked.removeAt(chunked.size - 1)
            } else
            {
                strayComponent = listOf()
            }

            tournament.roundNumber++
            tournament.expectedMatchList = ScheduledMatchList(
                playerGroups = chunked,
                stray = strayComponent
            )
        }
    }

    data object InvokeStartOnMatchList : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {
            val matchList = tournament.expectedMatchList
                ?: return

            DPSRedisShared.sendMessage(
                matchList.stray.flatMap(TournamentMember::players),
                listOf(
                    "&cDue to an uneven number of tournament members, you will not be participating in tournament round #${
                        tournament.roundNumber
                    }."
                )
            )

            tournament.currentMatchList.clear()
            matchList.playerGroups.forEach {
                val kit = KitDataSync.cached()
                    .kits[tournament.config.kitID]
                    ?: return@forEach

                val randomMap = MapDataSync
                    .selectRandomMapCompatibleWith(kit)
                    ?: return@forEach

                val expectation = GameExpectation(
                    identifier = UUID.randomUUID(),
                    players = it.flatMap(TournamentMember::players),
                    teams = mapOf(
                        GameTeamSide.A to GameTeam(
                            side = GameTeamSide.A,
                            players = it.first().players.toList()
                        ),
                        GameTeamSide.B to GameTeam(
                            side = GameTeamSide.B,
                            players = it.last().players.toList()
                        ),
                    ),
                    kitId = tournament.config.kitID,
                    mapId = randomMap.name,
                    queueId = "tournament"
                )

                tournament.currentMatchList += expectation
            }

            CompletableFuture
                .allOf(
                    *(tournament.currentMatchList.map {
                        val map = MapDataSync.cached().maps[it.mapId]
                            ?: return@map CompletableFuture
                                .completedFuture(null)

                        GameQueueManager.prepareGameFor(
                            map = map,
                            expectation = it,
                            region = tournament.config.region,
                            cleanup = { }
                        )
                    }.toTypedArray())
                )
                .join()
        }
    }

    data object FinalizeTournamentAndDispose : SideEffect()
    {
        override fun invoke(tournament: Tournament)
        {
            // TODO: someone make this better please
            println(tournament.memberSet)

            DPSRedisShared.sendBroadcast(
                Message()
                    .withMessage("&a&lThe tournament has ended!")
                    .withMessage(
                        "&aWinners: &f${
                            tournament.memberSet.firstOrNull()?.players
                                ?.joinToString(
                                    separator = ", ",
                                    transform = {
                                        ScalaStoreUuidCache.username(it) ?: "??"
                                    }
                                )
                                ?: "Someone"
                        }"
                    )
            )

            tournament.stop()
        }
    }
}
