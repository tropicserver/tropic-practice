package gg.tropic.practice.scoreboard

import gg.scala.lemon.LemonConstants
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
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
        board += "${CC.WHITE}Online: ${CC.PRI}0"
        board += "${CC.WHITE}Playing: ${CC.PRI}0"

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
                DevInfoService.devInfo.gameServers
            }"
            board += "${CC.WHITE}  Queued: ${CC.GOLD}${
                DevInfoService.devInfo.queued
            }"
            board += "${CC.WHITE}  Mean TPS: ${CC.GREEN}${
                "%.2f".format(DevInfoService.devInfo.meanTPS)
            }"
            board += "${CC.WHITE}Available Rpls.: ${CC.PRI}${
                DevInfoService.devInfo.availableReplications
            }"
        }

        board += ""
        board += "${CC.GRAY}${LemonConstants.WEB_LINK}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}  ${CC.GRAY}"
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}DUELS ${CC.GRAY}(beta)"
}
