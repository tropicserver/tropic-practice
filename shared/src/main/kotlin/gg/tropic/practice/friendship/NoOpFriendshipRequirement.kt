package gg.tropic.practice.friendship

import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
object NoOpFriendshipRequirement : FriendshipRequirement
{
    override fun existsBetween(playerOne: UUID, playerTwo: UUID) =
        CompletableFuture.completedFuture(true)
}
