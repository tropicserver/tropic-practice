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
object DuelsNametagImpl : NametagProvider("practice", Int.MAX_VALUE)
{
    override fun fetchNametag(
        toRefresh: Player, refreshFor: Player
    ): NametagInfo?
    {
        val playerGame = GameService
            .byPlayerOrSpectator(toRefresh.uniqueId)
            ?: return null

        val targetGame = GameService
            .byPlayerOrSpectator(refreshFor.uniqueId)
            ?: return null

        if (targetGame.expectation == playerGame.expectation)
        {
            if (refreshFor.hasMetadata("spectator"))
            {
                return createNametag(CC.GRAY, "", "zzz")
            }

            return runCatching {
                if (
                    playerGame.getTeamOf(refreshFor).side ==
                    playerGame.getTeamOf(toRefresh).side
                )
                {
                    createNametag(CC.GREEN, "")
                } else
                {
                    createNametag(CC.RED, "")
                }
            }.getOrNull() ?: createNametag(
                CC.GREEN, ""
            )
        }

        return null
    }
}
