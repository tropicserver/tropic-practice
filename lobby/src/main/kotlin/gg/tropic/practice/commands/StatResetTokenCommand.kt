package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.commands.admin.ResetStatsCommand
import gg.tropic.practice.statresets.StatResetTokens
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 7/12/2023
 */
@AutoRegister
@CommandAlias("statresettokens|srtokens|statresets")
object StatResetTokenCommand : ScalaCommand()
{
    @AssignPermission
    @CommandCompletion("@players")
    @Subcommand("give")
    fun onGive(sender: CommandSender, target: AsyncLemonPlayer, amount: Int) =
        target.validatePlayers(sender, true) {
            ScalaCommonsSpigot.instance.kvConnection
                .sync()
                .hincrby(
                    "tropicpractice:statreset-tokens:tokens",
                    it.uniqueId.toString(),
                    amount.toLong()
                )

            sender.sendMessage(
                "${CC.GREEN}Added ${Numbers.format(amount)} stat reset tokens to ${CC.AQUA}${it.name}'s${CC.GREEN} account. ${CC.GRAY}"
            )
        }

    @Default
    fun onShow(player: ScalaPlayer) = StatResetTokens.of(player.uniqueId)
        .thenAccept {
            it ?: throw ConditionFailedException(
                "You do not have any stat reset tokens!"
            )

            player.sendMessage(
                "${CC.GREEN}You have ${CC.WHITE}${Numbers.format(it)}${CC.GREEN} stat reset tokens!",
                "${CC.GRAY}Use ${CC.AQUA}/statresets use${CC.GRAY} to reset your stats!"
            )
        }

    @Default
    fun onUse(player: ScalaPlayer, use: String) = player.let {
        if (use != "use")
        {
            onShow(player)
            return@let CompletableFuture.completedFuture(null)
        }

        val tokens = StatResetTokens.of(player.uniqueId)
            .join()
            ?: throw ConditionFailedException(
                "You do not have any stat reset tokens!"
            )

        if (tokens < 1)
        {
            throw ConditionFailedException(
                "You do not have any stat reset tokens to use!"
            )
        }

        ScalaCommonsSpigot.instance.kvConnection
            .sync()
            .hincrby(
                "statreset-tokens:tokens",
                player.uniqueId.toString(),
                -1L
            )

        return@let ResetStatsCommand
            .onResetStats(
                Bukkit.getConsoleSender(),
                spec = ResetStatsCommand.StatReset.AllStats,
                target = AsyncLemonPlayer.of(player.uniqueId),
                kit = null
            )
            .thenRun {
                player.sendMessage(
                    "${CC.GREEN}You have used up one of your stat reset tokens."
                )
            }
    }
}
