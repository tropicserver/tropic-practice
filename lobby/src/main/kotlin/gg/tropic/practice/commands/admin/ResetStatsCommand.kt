package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.commands.offlineProfile
import gg.tropic.practice.statistics.GlobalStatistics
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import net.evilblock.cubed.util.CC
import org.bukkit.command.CommandSender

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@AutoRegister
object ResetStatsCommand : ScalaCommand()
{
    @CommandAlias("resetstats")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.resetstats")
    fun onResetStats(player: CommandSender, target: AsyncLemonPlayer) =
        target.validatePlayers(player, false) {
            val profile = it.uniqueId.offlineProfile
            profile.casualStatistics.keys.forEach { key ->
                profile.casualStatistics[key] = KitStatistics()
            }

            profile.rankedStatistics.keys.forEach { key ->
                profile.rankedStatistics[key] = RankedKitStatistics()
            }

            profile.globalStatistics = GlobalStatistics()
            profile.save().join()

            player.sendMessage(
                "${CC.GREEN}You have reset the stats for ${CC.WHITE}${it.name}${CC.GREEN}."
            )
        }
}
