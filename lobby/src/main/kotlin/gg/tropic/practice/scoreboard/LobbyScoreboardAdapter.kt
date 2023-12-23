package gg.tropic.practice.scoreboard

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.lemon.LemonConstants
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.settings.scoreboard.LobbyScoreboardView
import gg.tropic.practice.player.LobbyPlayerService
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@ScoreboardAdapterRegister
object LobbyScoreboardAdapter : ScoreboardAdapter()
{
    override fun getLines(board: LinkedList<String>, player: Player)
    {
        val profile = LobbyPlayerService.find(player.uniqueId)
            ?: return
        board += ""
        board += "${CC.WHITE}Online: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.online)
        }"
        board += "${CC.WHITE}Playing: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.playing)
        }"

        if (profile.inQueue())
        {
            board += ""
            board += "${CC.PRI}${profile.queuedForType().name} Queue:"
            board += "${CC.GRAY}${profile.queuedForKit()?.displayName} ${
                profile.queuedForTeamSize()
            }v${
                profile.queuedForTeamSize()
            }"
            board += "${CC.WHITE}Queued for ${CC.PRI}${
                TimeUtil.formatIntoMMSS((profile.queuedForTime() / 1000).toInt())
            }"
        }

        BasicsProfileService.find(player)
            ?.apply {
                val scoreboardView = setting(
                    "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:lobby-scoreboard-view",
                    LobbyScoreboardView.None
                )

                if (scoreboardView != LobbyScoreboardView.None)
                {
                    board += ""
                    board += "${CC.PRI}${scoreboardView.displayName}:"
                }

                when (scoreboardView)
                {
                    LobbyScoreboardView.Dev ->
                    {
                        board += "${CC.WHITE}Game servers: ${CC.PRI}${
                            ScoreboardInfoService.scoreboardInfo.gameServers
                        }"
                        board += "${CC.GRAY}  Mean TPS: ${CC.GREEN}${
                            "%.2f".format(ScoreboardInfoService.scoreboardInfo.meanTPS)
                        }"
                        board += "${CC.WHITE}Available Rpls.: ${CC.PRI}${
                            ScoreboardInfoService.scoreboardInfo.availableReplications
                        }"
                    }

                    LobbyScoreboardView.Staff ->
                    {
                        fun metadataDisplay(metadata: String) = if (player.hasMetadata(metadata))
                            "${CC.GREEN}Enabled" else "${CC.RED}Disabled"

                        board += "${CC.WHITE}Vanish: ${metadataDisplay("vanished")}"
                        board += "${CC.WHITE}Mod Mode: ${metadataDisplay("mod-mode")}"
                    }

                    LobbyScoreboardView.None ->
                    {

                    }
                }
            }

        board += ""
        board += CC.GRAY + LemonConstants.WEB_LINK + "          " + CC.GRAY + "      " + CC.GRAY + "  " + CC.GRAY
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}Practice ${CC.GRAY}(beta)"
}
