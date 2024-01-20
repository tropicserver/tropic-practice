package gg.tropic.practice.friendship

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
interface FriendshipRequirement
{
    fun existsBetween(playerOne: UUID, playerTwo: UUID): CompletableFuture<Boolean>
}
