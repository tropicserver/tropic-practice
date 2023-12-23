package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@AutoRegister
object RegionCommand : ScalaCommand()
{
    @CommandAlias("region|myregion")
    fun onRegion(player: ScalaPlayer) = PlayerRegionFromRedisProxy
        .of(player.bukkit())
        .thenAccept {
            player.sendMessage(
                "${CC.GREEN}You are connected to the ${CC.PRI}${it.name}${CC.GREEN} region."
            )
        }
}
