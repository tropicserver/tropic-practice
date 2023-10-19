package gg.tropic.practice.commands

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import net.evilblock.cubed.util.CC

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object SpectateCommad : ScalaCommand()
{
    @CommandAlias(
        "spec|spectate"
    )
    fun onPlayerSpectate(player: ScalaPlayer, target: ScalaPlayer)
    {
        val profile = BasicsProfileService.find(target.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, ${target.bukkit().name}'s profile did not load properly."
            )

        val allowSpectators = profile.settings["duels:allow-spectators"]!!
        if (allowSpectators.value == "DISABLED")
        {
            player.sendMessage(
                "${CC.RED}This player does not allow spectators."
            )
            return
        }

        // Needs further implementation once the spectation system is finished
    }
}
