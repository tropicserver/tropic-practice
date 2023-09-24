package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.editor.EditLoadoutContentsMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.profile.loadout.Loadout


@AutoRegister
@CommandAlias("kiteditor")
object KitEditorCommand : ScalaCommand()
{
    @Default
    fun onKitEditor(player: ScalaPlayer)
    {
        val profile = PracticeProfileService.find(player.uniqueId)
            ?: throw ConditionFailedException(
                "You profile doesn't exist!"
            )

        // purely for testing
        EditorKitSelectionMenu(profile).openMenu(player.bukkit())
    }
}
