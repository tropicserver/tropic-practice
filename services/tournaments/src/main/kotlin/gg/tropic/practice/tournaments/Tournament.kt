package gg.tropic.practice.tournaments

import com.tinder.StateMachine
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.game.GameExpectation
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.serializable.Message
import gg.tropic.practice.suffixWhenDev
import net.md_5.bungee.api.chat.ClickEvent
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
class Tournament(val config: TournamentConfig) : () -> Unit
{
    var currentRoundLosers: MutableSet<UUID> = mutableSetOf()
    private var ticker: ScheduledFuture<*>? = null

    var memberSet = mutableSetOf<TournamentMember>()

    var currentMatchList = mutableListOf<GameExpectation>()

    var expectedMatchList: ScheduledMatchList? = null
    var roundNumber = 0

    val stateMachine = StateMachine
        .create<TournamentState, StateEvent, SideEffect> {
            initialState(TournamentState.Populating)

            state<TournamentState.Ended> {

            }

            state<TournamentState.Populating> {
                on<StateEvent.OnPopulated> {
                    transitionTo(
                        TournamentState.RoundStarting,
                        SideEffect.DetermineNextMatchList
                    )
                }
            }

            state<TournamentState.RoundStarting> {
                on<StateEvent.OnRoundsStarted> {
                    transitionTo(
                        TournamentState.RoundInProgress,
                        SideEffect.InvokeStartOnMatchList
                    )
                }
            }

            state<TournamentState.RoundInProgress> {
                on<StateEvent.OnRoundsEnded> {
                    val previousMembers = memberSet.toSet()
                    memberSet = previousMembers
                        .filter { member ->
                            member.players.intersect(currentRoundLosers).isEmpty()
                        }
                        .toMutableSet()
                    currentRoundLosers = mutableSetOf()

                    val requiresMoreBrackets = memberSet.size > 1
                    if (requiresMoreBrackets)
                    {
                        val advancingPlayers = memberSet
                            .flatMap(TournamentMember::players)

                        DPSRedisShared.sendMessage(
                            advancingPlayers,
                            listOf(
                                "&a&lRound #$roundNumber has ended! &f${
                                    advancingPlayers.size
                                }&a players will be moving on to the next round."
                            )
                        )

                        return@on transitionTo(
                            TournamentState.RoundStarting,
                            SideEffect.DetermineNextMatchList
                        )
                    }

                    transitionTo(
                        TournamentState.Ended,
                        SideEffect.FinalizeTournamentAndDispose
                    )
                }
            }

            onTransition {
                val validTransition = it as? StateMachine.Transition.Valid
                    ?: return@onTransition

                validTransition.sideEffect?.invoke(this@Tournament)

                lastBroadcast = -1L
                currentTick = 30
            }
        }

    fun startResources()
    {
        check(ticker == null)
        this.ticker = TournamentManager.scheduleAtFixedRate(
            {
                runCatching { this@Tournament() }
                    .onFailure(Throwable::printStackTrace)
            }, 0L, 1L, TimeUnit.SECONDS
        )

        joinTournament(config.creator)
    }

    fun stop()
    {
        ticker?.cancel(true)
        currentMatchList.forEach {
            TournamentManager.redis
                .createMessage(
                    "terminate",
                    "matchID" to it.identifier
                )
                .publish(
                    AwareThreadContext.ASYNC,
                    channel = "practice:communications".suffixWhenDev()
                )
        }

        TournamentManager.activeTournament = null
    }

    fun leaveTournament(uniqueId: UUID)
    {
        // TODO: support multi player teams
        this.memberSet.removeIf {
            it.leader == uniqueId
        }
    }

    fun joinTournament(uniqueId: UUID)
    {
        // TODO: support multi player teams
        this.memberSet += TournamentMember(
            leader = uniqueId,
            players = setOf(uniqueId)
        )

        DPSRedisShared.sendMessage(
            memberSet.flatMap(TournamentMember::players).toList(),
            Message()
                .withMessage(
                    "{primary}${
                        ScalaStoreUuidCache.username(uniqueId)
                    }&e joined the tournament. &7(${
                        memberSet.size
                    }/${
                        config.maxPlayers
                    })"
                )
        )
    }

    private var lastBroadcast = -1L
    private var currentTick = 30

    private val broadcastSeconds = listOf(1, 2, 3, 4, 5, 10, 15, 20, 25, 30)

    override fun invoke()
    {
        when (stateMachine.state)
        {
            TournamentState.Populating ->
            {
                if (memberSet.size >= config.maxPlayers)
                {
                    stateMachine.transition(StateEvent.OnPopulated)
                    return
                }

                if (System.currentTimeMillis() - lastBroadcast > 10_000L)
                {
                    sendStartingBroadcast()
                    lastBroadcast = System.currentTimeMillis()
                }
            }

            TournamentState.RoundInProgress ->
            {
                val statuses = GameManager.allGames().join()
                val associated = statuses.associateBy { it.uniqueId }

                val ongoingMatches = currentMatchList
                    .filter {
                        associated.containsKey(it.identifier)
                    }

                if (ongoingMatches.isEmpty())
                {
                    stateMachine.transition(StateEvent.OnRoundsEnded)
                    return
                }
            }

            TournamentState.RoundStarting ->
            {
                if (currentTick-- >= 0)
                {
                    if (currentTick in broadcastSeconds)
                    {
                        DPSRedisShared.sendMessage(
                            memberSet
                                .flatMap(TournamentMember::players)
                                .toList(),
                            listOf(
                                "{secondary}Round {primary}&l#$roundNumber{secondary} starts in {primary}$currentTick seconds{secondary}."
                            )
                        )
                    }
                    return
                }

                stateMachine.transition(StateEvent.OnRoundsStarted)

                DPSRedisShared.sendBroadcast(
                    Message()
                        .withMessage("&aRound &l#$roundNumber&a has started!")
                        // TODO: implement this
                        /*.withMessage(
                            "&a&l(click to view info)"
                        )
                        .andHoverOf(
                            "Click to view info!"
                        )
                        .andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tournament view"
                        )*/
                )
            }

            TournamentState.Ended ->
            {

            }
        }
    }

    private fun sendStartingBroadcast()
    {
        val kit = KitDataSync.cached().kits[config.kitID]
            ?: return

        DPSRedisShared.sendBroadcast(
            Message()
                .withMessage(
                    " ",
                    "{primary}Tournament:",
                    "&7┃ &fHosted by: {primary}${
                        ScalaStoreUuidCache.username(config.creator)
                    }",
                    "&7┃ &fKit: {primary}${kit.displayName}",
                    "&7┃ &fRegion: {primary}${config.region.name}",
                    "&7┃ &fPlayers: {primary}${memberSet.size}/${config.maxPlayers}",
                    " "
                )
                .withMessage(
                    "&a(Click to join)"
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/tournament join"
                )
                .andHoverOf("Click to join!")
                .withMessage("")
        )
    }

    fun isInTournament(player: UUID) = memberSet.any { player in it.players }
}
