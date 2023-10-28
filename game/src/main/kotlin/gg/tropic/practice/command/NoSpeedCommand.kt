package gg.tropic.practice.command

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import net.evilblock.cubed.util.CC
import org.bukkit.potion.PotionEffectType

/**
 * @author GrowlyX
 * @since 10/28/2023
 */
@AutoRegister
object NoSpeedCommand : ScalaCommand()
{
    @CommandAlias("nospeed")
    fun onNoSpeed(player: ScalaPlayer)
    {
        if (!player.bukkit().hasPotionEffect(PotionEffectType.SPEED))
        {
            throw ConditionFailedException("You do not have speed!")
        }

        player.bukkit().removePotionEffect(PotionEffectType.SPEED)
        player.sendMessage("${CC.RED}You no longer have speed!")
    }
}
