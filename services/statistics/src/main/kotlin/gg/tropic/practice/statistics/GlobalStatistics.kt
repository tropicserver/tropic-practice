package gg.tropic.practice.statistics

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class GlobalStatistics
{
    var totalPlays = 0
        private set
    var totalWins = 0
        private set
    var totalLosses = 0
        private set
    var totalKills = 0
        private set
    var totalDeaths = 0
        private set

    fun sharedGamePlays() = ApplyUpdatesStateless(listOf { totalPlays++ })

    fun userPlayedGameAndWon() = ApplyUpdatesStateless(
        listOf {
            totalWins++
        },
        sharedGamePlays()
    )

    fun userPlayedGameAndLost() = ApplyUpdatesStateless(
        listOf {
            totalLosses++
        },
        sharedGamePlays()
    )

    fun userPlayedGameAndLostUserDied() = ApplyUpdatesStateless(
        listOf({
            totalLosses++
        }, {
            totalKills++
        }),
        sharedGamePlays()
    )

    fun userKilledOpponent() = ApplyUpdatesStateless(
        listOf {
            totalKills++
        }
    )

    fun userWonGameAndKilledOpponent() = ApplyUpdatesStateless(
        listOf({
            totalWins++
        }, {
            totalKills++
        }),
        sharedGamePlays()
    )
}
