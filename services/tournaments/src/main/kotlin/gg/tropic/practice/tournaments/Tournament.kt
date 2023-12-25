package gg.tropic.practice.tournaments

import com.tinder.StateMachine
import gg.tropic.practice.expectation.GameExpectation
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
    private val players = mutableSetOf<UUID>()
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

    fun run()
    {
        check(ticker == null)
        this.ticker = TournamentManager.scheduleAtFixedRate(
            this, 0L, 1L, TimeUnit.SECONDS
        )
    }

    fun join(team: TournamentMember)
    {

    }

    override fun invoke()
    {

    }
}
