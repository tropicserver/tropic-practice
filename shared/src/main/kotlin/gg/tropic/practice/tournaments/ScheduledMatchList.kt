package gg.tropic.practice.tournaments

/**
 * @author GrowlyX
 * @since 12/25/2023
 */
data class ScheduledMatchList(
    val playerGroups: List<List<TournamentMember>>,
    val stray: List<TournamentMember>
)
