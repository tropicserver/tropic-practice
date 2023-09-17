package gg.tropic.practice.resources

import gg.tropic.practice.games.GameService
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.nametag.NametagProviderRegister
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 8/9/2022
 */
@NametagProviderRegister
object DuelsNametagImpl : NametagProvider("duels", 3000000)
{
    override fun fetchNametag(
        toRefresh: Player, refreshFor: Player
    ): NametagInfo?
    {
        val playerGame = GameService
            .byPlayer(refreshFor)
            ?: return null

        val targetGame = GameService
            .byPlayer(toRefresh)
            ?: return null

        if (targetGame.expectation == playerGame.expectation)
        {
            if (
                toRefresh.hasMetadata("spectator")
            )
            {
                return createNametag(CC.GRAY, "")
            }

            return if (
                playerGame.getTeamOf(refreshFor).side ==
                playerGame.getTeamOf(toRefresh).side
            )
            {
                createNametag(CC.GREEN, "")
            } else
            {
                createNametag(CC.RED, "")
            }
        }

        return null
    }
}
