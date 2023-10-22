package gg.tropic.practice.duel

import gg.tropic.practice.PracticeShared
import gg.tropic.practice.games.DuelRequest
import gg.tropic.practice.kit.Kit
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 10/21/2023
 */
object DuelRequestUtilities
{
    fun duelRequestExists(sender: UUID, target: UUID, kit: Kit) = CompletableFuture
        .supplyAsync {
            ScalaCommonsSpigot
                .instance.kvConnection
                .sync()
                .hexists(
                    "${PracticeShared.KEY}:duelrequests:${sender}:${kit.id}",
                    target.toString()
                )
        }

    fun duelRequest(sender: UUID, target: UUID, kit: Kit) = CompletableFuture
        .supplyAsync {
            val request = ScalaCommonsSpigot
                .instance.kvConnection
                .sync()
                .hget(
                    "${PracticeShared.KEY}:duelrequests:${sender}:${kit.id}",
                    target.toString()
                )

            if (request != null)
            {
                Serializers.gson.fromJson(
                    request, DuelRequest::class.java
                )
            } else
            {
                null
            }
        }
}
