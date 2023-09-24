package gg.tropic.practice.application.api

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object DPSRedisShared
{
    val keyValueCache = DPSRedisService("dpskv")
        .configure { internal().connect() }
}
