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
        board += "${CC.WHITE}Online: ${CC.PRI}0"
        board += "${CC.WHITE}Playing: ${CC.PRI}0"
        board += ""
        board += "${LemonConstants.WEB_LINK}  ${CC.GRAY}"
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}${LemonConstants.SERVER_NAME}"
}
