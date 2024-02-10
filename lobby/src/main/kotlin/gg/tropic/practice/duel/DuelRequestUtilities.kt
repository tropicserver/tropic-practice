package gg.tropic.practice.duel

import gg.tropic.practice.games.duels.DuelRequest
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.*
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
                    "${namespace().suffixWhenDev()}:duelrequests:${sender}:${kit.id}",
                    target.toString()
                )
        }

    fun duelRequest(sender: UUID, target: UUID, kit: Kit) = CompletableFuture
        .supplyAsync {
            val request = ScalaCommonsSpigot
                .instance.kvConnection
                .sync()
                .hget(
                    "${namespace().suffixWhenDev()}:duelrequests:${sender}:${kit.id}",
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
