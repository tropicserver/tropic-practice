package gg.tropic.practice.tournaments

import com.tinder.StateMachine
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.serializable.Message
import net.evilblock.cubed.ScalaCommonsSpigot
import net.md_5.bungee.api.chat.ClickEvent
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
class Tournament(private val config: TournamentConfig) : () -> Unit
{
    private var ticker: ScheduledFuture<*>? = null
    private val players = mutableSetOf<TournamentMember>()
    private var currentMatchList = mutableListOf<GameExpectation>()

    private val stateMachine = StateMachine
        .create<TournamentState, StateEvent, SideEffect> {
            initialState(TournamentState.Populating)

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
                    val requiresMoreBrackets = true
                    if (requiresMoreBrackets)
                    {
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
            }
        }

    fun startResources()
    {
        check(ticker == null)
        this.ticker = TournamentManager.scheduleAtFixedRate(
            this, 0L, 1L, TimeUnit.SECONDS
        )

        joinTournament(config.creator)
    }

    fun stop()
    {
        ticker?.cancel(true)
        currentMatchList.forEach {
            AwareMessage
                .of(
                    "terminate",
                    ScalaCommonsSpigot.instance.aware,
                    "matchID" to it.identifier
                )
                .publish(
                    AwareThreadContext.SYNC,
                    channel = "practice:communications"
                )
        }
    }

    fun joinTournament(uniqueId: UUID)
    {
        // TODO: support multi player teams
        this.players += TournamentMember(
            leader = uniqueId, players = setOf(uniqueId)
        )

        DPSRedisShared.sendMessage(
            players.flatMap(TournamentMember::players).toList(),
            Message()
                .withMessage("{primary}${
                    ScalaStoreUuidCache.username(uniqueId)
                }&e joined the tournament. &7(${
                    players.size
                }/${
                    config.maxPlayers
                })")
        )
    }

    private var lastBroadcast = -1L
    override fun invoke()
    {
        when (stateMachine.state)
        {
            TournamentState.Ended ->
            {

            }

            TournamentState.Populating ->
            {
                if (players.size >= config.maxPlayers)
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

            }

            TournamentState.RoundStarting ->
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
                    "&7┃ &fPlayers: {primary}${players.size}/${config.maxPlayers}",
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

    fun isInTournament(player: UUID) = players.any { player in it.players }
}
