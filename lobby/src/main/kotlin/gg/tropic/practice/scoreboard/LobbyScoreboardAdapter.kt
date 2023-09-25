package gg.tropic.practice.scoreboard

import gg.scala.lemon.LemonConstants
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
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
        board += ""
        board += "${CC.WHITE}Online: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.online)
        }"
        board += "${CC.WHITE}Playing: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.playing)
        }"
        board += "${CC.WHITE}Queued: ${CC.GOLD}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.queued)
        }"

        if (player.hasPermission("practice.staff"))
        {
            board += ""
            board += "${CC.GOLD}Staff:"
            board += "${CC.WHITE}Vanish: ${
                if (player.hasMetadata("vanished"))
                {
                    "${CC.GREEN}Enabled"
                } else
                {
                    "${CC.RED}Disabled"
                }
            }"
        }

        if (player.hasPermission("practice.devinfo"))
        {
            board += ""
            board += "${CC.GOLD}Dev:"
            board += "${CC.WHITE}Game servers: ${CC.PRI}${
                ScoreboardInfoService.scoreboardInfo.gameServers
            }"
            board += "${CC.WHITE}  Mean TPS: ${CC.GREEN}${
                "%.2f".format(ScoreboardInfoService.scoreboardInfo.meanTPS)
            }"
            board += "${CC.WHITE}Available Rpls.: ${CC.PRI}${
                ScoreboardInfoService.scoreboardInfo.availableReplications
            }"
        }

        board += ""
        board += "${CC.GRAY}${LemonConstants.WEB_LINK}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}"
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}DUELS ${CC.GRAY}(beta)"
}
