package gg.tropic.practice.commands

import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.duel.DuelRequestUtilities
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.pipeline.DuelRequestPipeline
import gg.tropic.practice.queue.QueueService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/14/2023
 */
@AutoRegister
object DuelCommands : ScalaCommand()
{
    @CommandCompletion("@players")
    @CommandAlias("accept")
    fun onDuelAccept(
        player: ScalaPlayer,
        target: AsyncLemonPlayer,
        kit: Kit
    ) = target.validatePlayers(
        player.bukkit(), false
    ) {
        it.identifier.offlineProfile

        val duelRequest = DuelRequestUtilities
            .duelRequest(it.uniqueId, player.uniqueId, kit)
            .join()
            ?: throw ConditionFailedException(
                "${CC.YELLOW}${it.name}${CC.RED} has not sent you a duel request with the kit ${CC.YELLOW}${kit.displayName}${CC.RED}."
            )

        QueueService.createMessage(
            "accept-duel",
            "request" to duelRequest
        ).publish(
            channel = "practice:queue"
        )
    }

    @CommandCompletion("@players")
    @CommandAlias("duel|fight|sendduelrequest")
    fun onDuelRequest(
        player: ScalaPlayer,
        target: AsyncLemonPlayer
    ) = target.validatePlayers(
        player.bukkit(), false
    ) {
        if (it.uniqueId == player.uniqueId)
        {
            throw ConditionFailedException(
                "You cannot send a duel request to yourself!"
            )
        }

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

        DuelRequestPipeline
            .build(it.identifier)
            .openMenu(player.bukkit())
    }
}
