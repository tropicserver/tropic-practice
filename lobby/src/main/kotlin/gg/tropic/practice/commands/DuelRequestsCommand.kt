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
 * @author GrowlyX
 * @since 8/5/2022
 */
@AutoRegister
object DuelRequestsCommand : ScalaCommand()
{
    @CommandAlias(
        "tdr|toggleduelrequests|duelrequests"
    )
    fun onDuelRequests(player: ScalaPlayer)
    {
        val profile = BasicsProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, your profile did not load properly."
            )

        val messagesRef = profile.settings["${DuelsSettingCategory.DUEL_SETTING_PREFIX}:duel-requests"]!!
        val mapped = messagesRef.map<StateSettingValue>()

        if (mapped == StateSettingValue.ENABLED)
        {
            messagesRef.value = "DISABLED"

            player.sendMessage(
                "${CC.RED}You're no longer receiving duel requests."
            )
        } else
        {
            messagesRef.value = "ENABLED"

            player.sendMessage(
                "${CC.GREEN}You've now receiving duel requests."
            )
        }

        profile.save()
    }
}
