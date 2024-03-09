package gg.tropic.practice.command

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.tropic.practice.autoscale.ReplicationAutoScaleTask
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.command.CommandSender

/**
 * @author GrowlyX
 * @since 3/8/2024
 */
@AutoRegister
object ReplicationHealthCommand : ScalaCommand()
{
    @CommandAlias("replicationhealth")
    @CommandPermission("op")
    fun onReplicationHealth(sender: CommandSender)
    {
        sender.sendMessage(
            "${CC.PRI}Last autoscale check: ${CC.GREEN}${
                TimeUtil.formatIntoAbbreviatedString(((System.currentTimeMillis() - ReplicationAutoScaleTask.lastAutoScaleCheck) / 1000).toInt())
            }"
        )

        sender.sendMessage(
            "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Last autoscale event: ${
                TimeUtil.formatIntoAbbreviatedString(((System.currentTimeMillis() - ReplicationAutoScaleTask.lastAutoScaleEvent) / 1000).toInt())
            }"
        )

        sender.sendMessage(
            "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Last autoscale event generation count: ${ReplicationAutoScaleTask.lastAutoScaleEventCount}"
        )
    }
}
