package gg.tropic.practice.commands

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.friendship.FriendshipStateSetting
import gg.tropic.practice.settings.DuelsSettingCategory
import net.evilblock.cubed.util.CC
import java.util.concurrent.CompletableFuture

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
    fun onDuelRequests(player: ScalaPlayer): CompletableFuture<Void>
    {
        val profile = BasicsProfileService.find(player.bukkit())
            ?: throw ConditionFailedException(
                "Sorry, your profile did not load properly."
            )

        val allowDuelRequests = profile.settings["${DuelsSettingCategory.DUEL_SETTING_PREFIX}:duel-requests-fr"]!!

        player.sendMessage(
            "${CC.GREEN}You have set your duel request settings to: ${
                when (allowDuelRequests.map<FriendshipStateSetting>())
                {
                    FriendshipStateSetting.Enabled ->
                    {
                        allowDuelRequests.value = "FriendsOnly"
                        "${CC.GOLD}Friends Only"
                    }
                    FriendshipStateSetting.FriendsOnly ->
                    {
                        allowDuelRequests.value = "Disabled"
                        "${CC.RED}Disabled"
                    }
                    FriendshipStateSetting.Disabled ->
                    {
                        allowDuelRequests.value = "Enabled"
                        "${CC.GREEN}Enabled"
                    }
                }
            }"
        )

        return profile.save()
    }
}
