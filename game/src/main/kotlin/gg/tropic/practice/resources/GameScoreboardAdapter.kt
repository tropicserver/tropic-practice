package gg.tropic.practice.resources

import gg.scala.flavor.service.Service
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.services.ScoreboardTitleService
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
@Service
@ScoreboardAdapterRegister
object GameScoreboardAdapter : ScoreboardAdapter()
{
    override fun getLines(
        board: LinkedList<String>, player: Player
    )
    {
        val game = GameService
            .byPlayerOrSpectator(player.uniqueId)
            ?: return

        board += ""

        if (player.uniqueId in game.expectedSpectators)
        {
            for (team in game.teams.values)
            {
                fun Player.format() = "${CC.WHITE}${
                    if (hasMetadata("spectator")) CC.STRIKE_THROUGH else ""
                }$name ${CC.GRAY}(${
                    MinecraftReflection.getPing(this)
                }ms)"

                val bukkitPlayers = team.toBukkitPlayers()
                    .filterNotNull()
                val sidePrefix = if (team.side == GameTeamSide.A)
                    "${CC.GREEN}[A]:" else "${CC.RED}[B]:"

                board += "$sidePrefix ${
                    if (bukkitPlayers.size == 1) bukkitPlayers.first().format() else ""
                }"

                if (bukkitPlayers.size > 1)
                {
                    bukkitPlayers
                        .take(3)
                        .forEach {
                            board += "- ${it.format()}"
                        }

                    board += ""
                }
            }

            if (board.last() != "")
            {
                board += ""
            }

            board += "Map: ${CC.PRI}${game.map.displayName}"
            board += "Type: ${CC.PRI}${game.expectationModel.queueType ?: "Duel"}"
            board += "Duration: ${CC.PRI}${game.getDuration()}"
        } else
        {
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
                        board += "${CC.WHITE}Opponent: ${CC.PRI}${
                            opponent.players.first().username()
                        }"
                    } else
                    {
                        board += "${CC.PRI}Opponents:"

                        for (other in opponent.players.take(3))
                        {
                            board += " - ${other.username()}"
                        }

                        if (opponent.players.size > 3)
                        {
                            board += "${CC.GRAY}(and ${opponent.players.size - 3} more...)"
                        }

                        board += ""
                    }

                    board += "${CC.WHITE}Map: ${CC.PRI}${game.map.displayName}"
                }

                GameState.Playing ->
                {
                    val opponent = game.getOpponent(player)
                        ?: return

                    board += "${CC.WHITE}Duration: ${CC.PRI}${game.getDuration()}"

                    val showHitScoreboard = game.flag(FeatureFlag.WinWhenNHitsReached)

                    if (opponent.players.size == 1)
                    {
                        val opponentPlayer = opponent.players.first()

                        board += "${CC.WHITE}Opponent: ${CC.PRI}${opponentPlayer.username()}"
                        board += ""

                        if (!showHitScoreboard)
                        {
                            board += "${CC.WHITE}Your ping: ${CC.GREEN}${MinecraftReflection.getPing(player)}ms"
                            board += "${CC.WHITE}Their ping: ${CC.RED}${
                                if (Bukkit.getPlayer(opponentPlayer) != null)
                                    MinecraftReflection.getPing(
                                        Bukkit.getPlayer(opponentPlayer)
                                    ) else "0"
                            }ms"
                        } else
                        {
                            val teamOfPlayer = game.getTeamOf(player)
                            val teamOfOpponent = game.getOpponent(player)!!
                            val hitsDiff = teamOfPlayer.combinedHits - teamOfOpponent.combinedHits

                            val playerCombo = teamOfPlayer.playerCombos[player.uniqueId] ?: 0

                            board += "${CC.WHITE}Your hits: ${CC.GREEN}${
                                teamOfPlayer.combinedHits
                            }${
                                if (hitsDiff == 0)
                                    ""
                                else
                                    if (hitsDiff > 0)
                                        " ${CC.GREEN}(+$hitsDiff)"
                                    else
                                        " ${CC.RED}($hitsDiff)"
                            }"
                            board += " ${
                                if (playerCombo == 0)
                                    "${CC.D_GRAY}No combo"
                                else
                                    if (playerCombo > 0)
                                        "${CC.GREEN}+$playerCombo combo"
                                    else 
                                        ""
                            }"

                            board += "${CC.WHITE}Enemy hits: ${CC.RED}${
                                teamOfOpponent.combinedHits
                            }"
                        }
                    } else
                    {
                        board += "${CC.WHITE}Map: ${CC.PRI}${game.map.displayName}"
                        board += ""

                        if (!showHitScoreboard)
                        {
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
                        } else
                        {
                            val teamOfPlayer = game.getTeamOf(player)
                            val teamOfOpponent = game.getOpponent(player)!!
                            val hitsDiff = teamOfPlayer.combinedHits - teamOfOpponent.combinedHits

                            board += "${CC.WHITE}Team: ${CC.PRI}${
                                teamOfPlayer.combinedHits
                            }${
                                if (hitsDiff == 0)
                                    ""
                                else
                                    if (hitsDiff > 0)
                                        " ${CC.GREEN}(+$hitsDiff)"
                                    else
                                        " ${CC.RED}($hitsDiff)"
                            }"

                            board += "${CC.RED}Opponent:"
                            board += "${teamOfOpponent.combinedHits} hits"

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
        }

        board += ""
        board += CC.GRAY + LemonConstants.WEB_LINK + "          " + CC.GRAY + "      " + CC.GRAY + "  " + CC.GRAY
    }

    override fun getTitle(player: Player) = ScoreboardTitleService.getCurrentTitle()
}
