package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.commands.offlineProfile
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.time.Duration
import net.evilblock.cubed.util.time.TimeUtil
import java.util.*

/**
 * @author GrowlyX
 * @since 1/1/2024
 */
@AutoRegister
object RankedBanCommand : ScalaCommand()
{
    @CommandAlias("rankedbanstatus")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.rankedbanstatus")
    fun onRankedBanStatus(
        player: ScalaPlayer,
        target: AsyncLemonPlayer,
    ) = target
        .validatePlayers(player.bukkit(), false) {
            val profile = it.uniqueId.offlineProfile
            if (profile.hasActiveRankedBan())
            {
                player.sendMessage(
                    "${CC.GREEN}Ranked ban effective ${
                        if (profile.rankedBanEffectiveUntil() == null)
                            "${CC.BOLD}forever" else "until ${
                            TimeUtil.formatIntoDateString(
                                Date(profile.rankedBanEffectiveUntil()!!)
                            )
                        }"
                    }"
                )
                return@validatePlayers
            }

            player.sendMessage("${CC.RED}Player has no active ranked ban.")
        }

    @CommandAlias("rankedunban")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.rankedban")
    fun onRankedUnBan(
        player: ScalaPlayer,
        target: AsyncLemonPlayer,
    ) = target
        .validatePlayers(player.bukkit(), false) {
            val profile = it.uniqueId.offlineProfile
            if (!profile.hasActiveRankedBan())
            {
                throw ConditionFailedException(
                    "The player ${CC.YELLOW}${it.name}${CC.RED} does not have an active ranked ban."
                )
            }

            profile.removeRankedBan()
            profile.saveAndPropagate().join()

            player.sendMessage(
                "${CC.RED}You have removed ${CC.YELLOW}${it.name}'s${CC.RED} ranked ban."
            )
        }

    @CommandAlias("rankedban")
    @CommandCompletion("@mip-players")
    @CommandPermission("practice.command.rankedunban")
    fun onRankedBan(
        player: ScalaPlayer,
        target: AsyncLemonPlayer,
        @Optional
        duration: Duration?
    ) = target
        .validatePlayers(player.bukkit(), false) {
            val profile = it.uniqueId.offlineProfile
            if (profile.hasActiveRankedBan())
            {
                throw ConditionFailedException(
                    "The player ${CC.YELLOW}${it.name}${CC.RED} already has an active ranked ban."
                )
            }

            profile.applyRankedBan(duration ?: Duration.PERMANENT)
            profile.saveAndPropagate().join()

            player.sendMessage(
                "${CC.GREEN}You have applied a ranked ban on ${CC.WHITE}${it.name}${CC.GREEN} for: ${CC.WHITE}${
                    if (duration == null) "forever" else TimeUtil
                        .formatIntoAbbreviatedString(
                            (duration.get() / 1000).toInt()
                        )
                }"
            )
        }
}
