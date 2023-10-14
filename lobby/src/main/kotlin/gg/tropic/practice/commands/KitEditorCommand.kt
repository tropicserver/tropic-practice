package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService

@AutoRegister
object KitEditorCommand : ScalaCommand()
{
    @CommandAlias("kiteditor")
    fun onKitEditor(player: ScalaPlayer)
    {
        val profile = PracticeProfileService.find(player.uniqueId)
            ?: throw ConditionFailedException(
                "You profile doesn't exist!"
            )

        with(LobbyPlayerService.find(player.bukkit())) {
            if (this?.state != PlayerState.Idle)
            {
                throw ConditionFailedException(
                    "You must be at spawn to enter the kit editor!"
                )
            }
        }

        EditorKitSelectionMenu(profile).openMenu(player.bukkit())
    }
}
