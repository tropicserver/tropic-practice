package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.DPSRedisService

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
object TournamentManager
{
    private val redis = DPSRedisService("tournaments")
        .apply(DPSRedisService::start)

    fun load()
    {
        redis.configure {
            listen("join") {

            }
        }
    }
}
