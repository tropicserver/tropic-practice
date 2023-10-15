package gg.tropic.practice.resources

import gg.scala.lemon.LemonConstants
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.scala.lemon.util.QuickAccess.username
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.nms.MinecraftReflection
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@ScoreboardAdapterRegister
object DuelsScoreboardImpl : ScoreboardAdapter()
{
    override fun getLines(
        board: LinkedList<String>, player: Player
    )
    {
        val game = GameService.byPlayer(player)
            ?: return

        board += ""

        when (game.state)
        {
            GameState.Waiting ->
            {
                board += "${CC.WHITE}Waiting for players..."
            }
            GameState.Starting ->
            {
                val opponent = game.getOpponent(player)
                    ?: return

                board += "${CC.WHITE}The game is starting!"
                board += ""

                if (opponent.players.size == 1)
                {
                    board += "${CC.WHITE}Opponent: ${CC.GOLD}${
                        opponent.players.first().username()
                    }"
                } else
                {
                    board += "${CC.GOLD}Opponents:"

                    for (other in opponent.players.take(3))
                    {
                        board += " - ${other.username()}"
                    }

                    if (opponent.players.size > 3)
                    {
                        board += "${CC.GRAY}(and ${opponent.players.size - 3} more...)"
                    }
                }
            }
            GameState.Playing ->
            {
                val opponent = game.getOpponent(player)
                    ?: return

                board += "${CC.WHITE}Duration: ${CC.GOLD}${game.getDuration()}"

                if (opponent.players.size == 1)
                {
                    val opponentPlayer = opponent.players.first()

                    board += "${CC.WHITE}Opponent: ${CC.GOLD}${opponentPlayer.username()}"
                    board += ""
                    board += "${CC.WHITE}Your ping: ${CC.GREEN}${MinecraftReflection.getPing(player)}ms"
                    board += "${CC.WHITE}Their ping: ${CC.RED}${
                        if (Bukkit.getPlayer(opponentPlayer) != null)
                            MinecraftReflection.getPing(
                                Bukkit.getPlayer(opponentPlayer)
                            ) else "0"
                    }ms"
                } else
                {
                    board += "${CC.WHITE}Map: ${CC.GOLD}${game.map.displayName}"
                    board += ""
                    board += "${CC.WHITE}Your ping: ${CC.GREEN}${MinecraftReflection.getPing(player)}ms"
                    board += "${CC.RED}Opponent pings:"

                    for (other in opponent.players.take(3))
                    {
                        val bukkitPlayer = Bukkit.getPlayer(other)
                            ?: continue

                        board += " - ${
                            if (bukkitPlayer.hasMetadata("spectator")) CC.STRIKE_THROUGH else ""
                        }${other.username()} ${CC.D_GRAY}(${
                            MinecraftReflection.getPing(bukkitPlayer)
                        }ms)"
                    }

                    if (opponent.players.size > 3)
                    {
                        board += "${CC.GRAY}(and ${opponent.players.size - 3} more...)"
                    }
                }
            }
            GameState.Completed ->
            {
                val report = game.report

                if (report != null)
                {
                    board += "${CC.WHITE}Winner: ${CC.GREEN}${
                        when (report.winners.size)
                        {
                            0 -> "N/A"
                            1 -> report.winners.first().username()
                            else ->
                            {
                                "${report.winners.first().username()}'s Team" 
                            }
                        }
                    }"
                    board += "${CC.WHITE}Loser: ${CC.RED}${
                        when (report.losers.size)
                        {
                            0 -> "N/A"
                            1 -> report.losers.first().username()
                            else ->
                            {
                                "${report.losers.first().username()}'s Team"
                            }
                        }
                    }"
                    board += ""
                    board += "${CC.WHITE}Thanks for playing!"
                } else
                {
                    board += "${CC.D_GRAY}Loading game report..."
                }
            }
        }

        board += ""

        // apparently this works?
        board += CC.GRAY + LemonConstants.WEB_LINK + "          "  + CC.GRAY + "      "  + CC.GRAY
    }

    override fun getTitle(player: Player) =
        "${CC.B_PRI}Practice ${CC.GRAY}(beta)"
}
