package gg.tropic.practice.application.api

import gg.scala.aware.thread.AwareThreadContext
import io.lettuce.core.api.StatefulRedisConnection
import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object DPSRedisShared
{
    val keyValueCache: StatefulRedisConnection<String, String> = DPSRedisService("dpskv")
        .configure { internal().connect() }

    private val lobbyBridge = DPSRedisService("lobbies")
        .apply(DPSRedisService::start)

    fun redirect(players: List<UUID>, server: String)
    {
        lobbyBridge.createMessage(
            packet = "redirect",
            "playerIDs" to players,
            "server" to server
        ).publish(
            AwareThreadContext.SYNC
        )
    }

    fun sendMessage(players: List<UUID>, messages: List<String>)
    {
        lobbyBridge.createMessage(
            packet = "send-messages",
            "playerIDs" to players,
            "message" to messages
        ).publish(
            AwareThreadContext.SYNC
        )
    }
}
