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
import java.util.concurrent.CompletableFuture

/**
 * @author Elb1to
 * @since 10/19/2023
 */
@AutoRegister
object ToggleSpectatorsCommand : ScalaCommand()
{
    @CommandAlias(
        "tsp|togglespecs|togglespectators"
    )
    fun onSpectateToggle(player: ScalaPlayer): CompletableFuture<Void>
    {
        val profile = BasicsProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, your profile did not load properly."
            )

        val allowSpectators = profile.settings["${DuelsSettingCategory.DUEL_SETTING_PREFIX}:allow-spectators"]!!
        val mapped = allowSpectators.map<StateSettingValue>()

        if (mapped == StateSettingValue.ENABLED)
        {
            allowSpectators.value = "DISABLED"
            player.sendMessage(
                "${CC.RED}Players are no longer able to spectate your matches."
            )
        } else
        {
            allowSpectators.value = "ENABLED"
            player.sendMessage(
                "${CC.GREEN}Players are now able to spectate your matches."
            )
        }

        return profile.save()
    }
}
