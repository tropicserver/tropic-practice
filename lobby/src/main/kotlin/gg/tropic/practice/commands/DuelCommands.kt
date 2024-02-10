package gg.tropic.practice.commands

import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.annotations.commands.HighPriority
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.scala.lemon.util.QuickAccess
import gg.tropic.practice.duel.DuelRequestUtilities
import gg.tropic.practice.friendship.FriendshipStateSetting
import gg.tropic.practice.friendship.Friendships
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.lobbyGroup
import gg.tropic.practice.menu.pipeline.DuelRequestPipeline
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 10/14/2023
 */
@AutoRegister
@HighPriority
object DuelCommands : ScalaCommand()
{
    @CommandCompletion("@mip-players")
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

        if (player.bukkit().hasMetadata("vanished"))
        {
            throw ConditionFailedException(
                "You are currently in vanish! Use ${CC.B}/vanish${CC.RED} to be able to accept a duel."
            )
        }

        if (player.bukkit().hasMetadata("frozen"))
        {
            throw ConditionFailedException(
                "You cannot accept this duel as you are frozen!"
            )
        }

        val profile = LobbyPlayerService.find(player.bukkit())
            ?: return@validatePlayers

        if (profile.state != PlayerState.Idle)
        {
            throw ConditionFailedException(
                "You are not in the right state to accept a duel!"
            )
        }

        QueueService.createMessage(
            "accept-duel",
            "request" to duelRequest
        ).publish(
            channel = "practice:queue".suffixWhenDev()
        )

        player.sendMessage("${CC.GREEN}Attempting to accept the duel...")
    }

    @CommandCompletion("@players")
    @CommandAlias("duel|d|fight|sendduelrequest")
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

        val profile = LobbyPlayerService.find(player.bukkit())
            ?: return@validatePlayers

        if (profile.state != PlayerState.Idle)
        {
            throw ConditionFailedException(
                "You cannot send a duel request right now!"
            )
        }

        val basicsProfile = it.identifier.basicsProfile
        it.identifier.offlineProfile

        val duelRequests = basicsProfile
            .setting<FriendshipStateSetting>(
                "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:duel-requests-fr"
            )

        if (duelRequests == FriendshipStateSetting.Disabled)
        {
            throw ConditionFailedException(
                "${CC.YELLOW}${it.name}${CC.RED} has their duel requests disabled!"
            )
        }

        if (duelRequests == FriendshipStateSetting.FriendsOnly)
        {
            val relationshipExists = Friendships.requirements
                .existsBetween(player.uniqueId, it.uniqueId)
                .join()

            if (!relationshipExists)
            {
                throw ConditionFailedException(
                    "You must be friends with ${CC.YELLOW}${it.name}${CC.RED} to send them a duel request!"
                )
            }
        }

        val server = QuickAccess.server(it.uniqueId)
            .join()
            ?: throw ConditionFailedException(
                "${CC.YELLOW}${it.name}${CC.RED} is not logged onto the network!"
            )

        if (lobbyGroup().suffixWhenDev() !in server.groups)
        {
            throw ConditionFailedException(
                "${CC.YELLOW}${it.name}${CC.RED} is not on a practice lobby!"
            )
        }

        DuelRequestPipeline
            .build(it.identifier)
            .openMenu(player.bukkit())
    }
}
