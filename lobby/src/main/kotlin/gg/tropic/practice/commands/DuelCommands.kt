package gg.tropic.practice.commands

import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.menu.pipes.DuelRequestPipe
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/14/2023
 */
@AutoRegister
object DuelCommands : ScalaCommand()
{
    @CommandCompletion("@players")
    @CommandAlias("duel|fight|sendduelrequest")
    fun onDuelRequest(
        player: ScalaPlayer,
        target: AsyncLemonPlayer
    ) = target.validatePlayers(
        player.bukkit(), false
    ) {
        val basicsProfile = it.identifier.basicsProfile
        it.identifier.offlineProfile

        val duelRequests = basicsProfile
            .setting<StateSettingValue>(
                "duels:duel-requests"
            )

        if (duelRequests != StateSettingValue.ENABLED)
        {
            throw ConditionFailedException(
                "${CC.YELLOW}${it.name}${CC.RED} has their duel requests disabled!"
            )
        }

        DuelRequestPipe
            .build(it.identifier)
            .openMenu(player.bukkit())
    }
}
