package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.scala.lemon.util.QuickAccess
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@AutoRegister
object RegionCommand : ScalaCommand()
{
    @CommandAlias("checkregion")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.checkregion")
    fun onCheckRegion(
        player: ScalaPlayer,
        target: AsyncLemonPlayer
    ) = target.validatePlayers(player.bukkit(), false) {
        val isPlayerOnline = QuickAccess
            .online(it.uniqueId)
            .join()

        if (!isPlayerOnline)
        {
            throw ConditionFailedException(
                "The player ${CC.YELLOW}${it.name}${CC.RED} is not logged onto the network."
            )
        }

        val region = PlayerRegionFromRedisProxy
            .ofPlayerID(it.uniqueId)
            .join()

        player.sendMessage(
            "${CC.GREEN}The player ${it.name} is logged onto: ${CC.PRI}$region"
        )
    }

    @CommandAlias("region|myregion")
    fun onRegion(player: ScalaPlayer) = PlayerRegionFromRedisProxy
        .of(player.bukkit())
        .thenAccept {
            player.sendMessage(
                "${CC.GREEN}You are connected to the ${CC.PRI}${it.name}${CC.GREEN} region."
            )
        }
}
