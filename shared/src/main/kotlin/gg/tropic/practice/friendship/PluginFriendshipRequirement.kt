package gg.tropic.practice.friendship

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.friends.handler.KFriendHandler
import java.util.*

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
@Service
@IgnoreAutoScan
@SoftDependency("Friends")
object PluginFriendshipRequirement : FriendshipRequirement
{
    @Configure
    fun configure()
    {
        Friendships.requirements = this
    }

    override fun existsBetween(playerOne: UUID, playerTwo: UUID) = KFriendHandler
        .getFriendshipBetween(playerOne, playerTwo)
        .thenApply {
            return@thenApply it != null
        }
}
