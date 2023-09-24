package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.TestQueueMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.profile.PracticeProfileService

@AutoRegister
object QueueTestCommand : ScalaCommand()
{
    @CommandAlias("testqueue")
    fun onKitEditor(player: ScalaPlayer)
    {
        TestQueueMenu().openMenu(player.bukkit())
    }
}
