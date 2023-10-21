package gg.tropic.practice.resources

import gg.tropic.practice.games.GameService
import net.evilblock.cubed.visibility.VisibilityAction
import net.evilblock.cubed.visibility.VisibilityAdapter
import net.evilblock.cubed.visibility.VisibilityAdapterRegister
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/9/2022
 */
@VisibilityAdapterRegister("duels")
object DuelsVisibilityImpl : VisibilityAdapter
{
    override fun getAction(
        toRefresh: Player, refreshFor: Player
    ): VisibilityAction
    {
        val playerGame = GameService
            .byPlayerOrSpectator(refreshFor.uniqueId)
            ?: return VisibilityAction.HIDE

        val targetGame = GameService
            .byPlayerOrSpectator(toRefresh.uniqueId)
            ?: return VisibilityAction.HIDE

        if (targetGame.expectation == playerGame.expectation)
        {
            if (
                toRefresh.hasMetadata("spectator") &&
                !refreshFor.hasMetadata("spectator")
            )
            {
                return VisibilityAction.HIDE
            }

            return VisibilityAction.NEUTRAL
        }

        return VisibilityAction.HIDE
    }
}
