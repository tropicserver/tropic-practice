package gg.tropic.practice.commands

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.settings.DuelsSettingCategory
import net.evilblock.cubed.util.CC

/**
 * @author Elb1to
 * @since 10/18/2023
 */
@AutoRegister
object SpawnFlightCommand : ScalaCommand()
{
    @CommandAlias(
        "tsp|togglespawnflight|toggleflight"
    )
    fun onFlightToggle(player: ScalaPlayer)
    {
        val profile = BasicsProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, your profile did not load properly."
            )

        val messagesRef = profile.settings["${DuelsSettingCategory.DUEL_SETTING_PREFIX}:spawn-flight"]!!
        val mapped = messagesRef.map<StateSettingValue>()

        if (mapped == StateSettingValue.ENABLED)
        {
            messagesRef.value = "DISABLED"

            player.bukkit().allowFlight = false
            player.bukkit().isFlying = false
            player.sendMessage(
                "${CC.RED}You can no longer fly around spawn."
            )
        } else
        {
            messagesRef.value = "ENABLED"

            player.bukkit().allowFlight = true
            player.bukkit().isFlying = true
            player.sendMessage(
                "${CC.GREEN}You can now fly around spawn."
            )
        }

        profile.save()
    }
}
