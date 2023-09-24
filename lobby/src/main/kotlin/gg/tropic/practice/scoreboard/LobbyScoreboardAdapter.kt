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

        // TODO: implement this all?
        if (player.hasPermission("practice.staff"))
        {
            board += ""
            board += "${CC.GOLD}Staff:"
            board += "${CC.WHITE}Staff Mode: ${CC.RED}Disabled"
            board += "${CC.WHITE}Vanish: ${CC.RED}Disabled"
        }

        // TODO: toggleable with command?
        if (player.hasPermission("practice.devinfo"))
        {
            board += ""
            board += "${CC.GOLD}Dev:"
            board += "${CC.WHITE}Game servers: ${CC.PRI}0"
            board += "${CC.WHITE}  Queued: ${CC.GOLD}0"
            board += "${CC.WHITE}  Mean TPS: ${CC.GREEN}*20.0"
            board += "${CC.WHITE}Available Rpls.: ${CC.PRI}0"
        }

        board += ""
        board += "${CC.GRAY}${LemonConstants.WEB_LINK}  ${CC.GRAY}"
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}DUELS ${CC.GRAY}(beta)"
}
