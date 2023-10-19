package gg.tropic.practice.commands

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.visibility.VisibilityHandler

/**
 * @author Elb1to
 * @since 10/18/2023
 */
@AutoRegister
object TogglePlayerVisibilityCommand : ScalaCommand()
{
    @CommandAlias(
        "tpv|toggleplayervisibility|togglevisibility"
    )
    fun onToggleVisibility(player: ScalaPlayer)
    {
        val profile = BasicsProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, your profile did not load properly."
            )

        val spawnVisibility = profile.settings["duels:spawn-visibility"]!!
        val mapped = spawnVisibility.map<StateSettingValue>()

        if (mapped == StateSettingValue.ENABLED)
        {
            spawnVisibility.value = "DISABLED"
            player.sendMessage(
                "${CC.RED}You can no longer see players at spawn."
            )
        } else
        {
            spawnVisibility.value = "ENABLED"
            player.sendMessage(
                "${CC.GREEN}You can now see players at spawn."
            )
        }

        VisibilityHandler.update(player.bukkit())
        profile.save()
    }
}
