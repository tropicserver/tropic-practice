package gg.tropic.practice.resources

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
        val game = GameService
            .byPlayer(player)
            ?: return kotlin.run {
                board.add("${CC.RED}No game found.")
            }

        board += ""

        when (game.state)
        {
            GameState.Generating ->
            {
                board += "${CC.GRAY}Generating the world..."
            }
            GameState.Waiting ->
            {
                board += "${CC.GRAY}Waiting for players..."
            }
            GameState.Starting ->
            {
                val opponent = game.getOpponent(player)
                    ?: return

                board += "${CC.GRAY}The game is starting!"
                board += ""

                if (opponent.players.size == 1)
                {
                    board += "${CC.GRAY}Opponent: ${CC.WHITE}${
                        opponent.players.first().username()
                    }"
                } else
                {
                    board += "${CC.GRAY}Opponents:"

                    for (other in opponent.players.take(3))
                    {
                        board += " - ${other.username()}"
                    }

                    if (opponent.players.size > 3)
                    {
                        board += "${CC.D_GRAY}(and ${opponent.players.size - 3} more...)"
                    }
                }
            }
            GameState.Playing ->
            {
                val opponent = game.getOpponent(player)
                    ?: return

                board += "${CC.GRAY}Duration: ${CC.WHITE}${game.getDuration()}"

                if (opponent.players.size == 1)
                {
                    val opponentPlayer = opponent.players.first()

                    board += "${CC.GRAY}Opponent: ${CC.WHITE}${opponentPlayer.username()}"
                    board += ""
                    board += "${CC.GREEN}Your ping: ${CC.WHITE}${MinecraftReflection.getPing(player)}ms"
                    board += "${CC.RED}Their ping: ${CC.WHITE}${
                        if (Bukkit.getPlayer(opponentPlayer) != null)
                            MinecraftReflection.getPing(
                                Bukkit.getPlayer(opponentPlayer)
                            ) else "0"
                    }ms"
                } else
                {
                    board += "${CC.GRAY}Arena: ${CC.WHITE}${game.map.display}"
                    board += ""
                    board += "${CC.GREEN}Your ping: ${CC.WHITE}${MinecraftReflection.getPing(player)}ms"
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
                        board += "${CC.D_GRAY}(and ${opponent.players.size - 3} more...)"
                    }
                }
            }
            GameState.Completed ->
            {
                val report = game.report

                if (report != null)
                {
                    board += "${CC.GREEN}Winner: ${CC.WHITE}${
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
                    board += "${CC.RED}Loser: ${CC.WHITE}${
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
                    board += "${CC.GRAY}Thanks for playing!"
                } else
                {
                    board += "${CC.D_GRAY}Loading game report..."
                }
            }
        }

        board += ""
        board += "???.com  ${CC.GRAY}"
    }

    override fun getTitle(player: Player) =
        "${CC.B_AQUA}???"
}
