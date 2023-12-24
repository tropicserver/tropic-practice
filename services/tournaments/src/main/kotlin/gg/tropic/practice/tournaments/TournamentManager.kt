package gg.tropic.practice.tournaments

import gg.tropic.practice.application.api.DPSRedisService
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

    fun load()
    {


        redis.configure {
            listen("join") {

            }
        }
    }
}
