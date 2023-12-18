package gg.tropic.practice.games.tasks

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.leaderboards.ScoreUpdates
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.evilblock.cubed.util.math.Numbers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class GameStopTask(
    private val game: GameImpl,
    private val report: GameReport,
    private val eloMappings: Map<UUID, Pair<Int, Int>>,
    private val positionUpdates: Map<UUID, CompletableFuture<ScoreUpdates>>
) : Runnable
{
    lateinit var task: Task

    override fun run()
    {
        kotlin.runCatching { this.runCatching() }
            .onFailure { it.printStackTrace() }
    }

    private fun runCatching()
    {
        if (this.game.activeCountdown == 5)
        {
            this.game.sendMessage(
                "",
                "${CC.PRI}Match Overview:",
                "${CC.I_GRAY}(Click to view inventories)",
                ""
            )

            val winnerComponent = FancyMessage()
                .withMessage(
                    "${CC.GREEN}Winner${
                        if (this.report.winners.size == 1) "" else "s"
                    }: ${CC.WHITE}"
                )

            if (this.report.winners.isEmpty())
            {
                winnerComponent.withMessage("${CC.RED}N/A")
            } else
            {
                for ((index, winner) in this.report.winners.withIndex())
                {
                    winnerComponent
                        .withMessage(winner.username())
                        .andHoverOf(
                            "${CC.GREEN}Click to view inventory!"
                        )
                        .andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/matchinventory ${this.report.identifier} $winner"
                        )

                    if (index < this.report.winners.size - 1)
                    {
                        winnerComponent.withMessage(", ")
                    }
                }
            }

            // this is redundant, but it's 1am. 1am growly is not a DRY growly.
            val loserComponent = FancyMessage()
                .withMessage(
                    "${CC.RED}Loser${
                        if (this.report.losers.size == 1) "" else "s"
                    }: ${CC.WHITE}"
                )

            if (this.report.losers.isEmpty())
            {
                loserComponent.withMessage("${CC.RED}N/A")
            } else
            {
                for ((index, loser) in this.report.losers.withIndex())
                {
                    loserComponent
                        .withMessage(loser.username())
                        .andHoverOf(
                            "${CC.GREEN}Click to view inventory!"
                        )
                        .andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/matchinventory ${this.report.identifier} $loser"
                        )

                    if (index < this.report.losers.size - 1)
                    {
                        loserComponent.withMessage(", ")
                    }
                }
            }

            this.game.sendMessage(
                winnerComponent,
                loserComponent
            )

            this.game.sendMessage("")

            if (game.expectedSpectators.isNotEmpty())
            {
                // TODO: exclude those who don't want to be shown
                game.sendMessage(
                    "${CC.YELLOW}Spectators ${CC.GRAY}(${
                        game.expectedSpectators.size
                    })${CC.YELLOW}: ${CC.WHITE}${
                        game.expectedSpectators.take(3)
                            .joinToString(", ") {
                                it.username()
                            }
                    }${
                        if (game.expectedSpectators.size > 3) " ${CC.GRAY}(and ${
                            game.expectedSpectators.size - 3
                        } more...)" else ""
                    }",
                    ""
                )
            }

            if (eloMappings.isNotEmpty())
            {
                val winner = eloMappings.keys.first()
                val loser = eloMappings.keys.last()
                game.sendMessage(
                    "${CC.PINK}ELO Updates:",
                    "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.GREEN}${winner.username()}:${CC.WHITE} ${eloMappings[winner]!!.first} ${CC.GRAY}(${CC.GREEN}+${eloMappings[winner]!!.second}${CC.GRAY})",
                    "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.RED}${loser.username()}:${CC.WHITE} ${eloMappings[loser]!!.first} ${CC.GRAY}(${CC.RED}-${eloMappings[loser]!!.second}${CC.GRAY})",
                    ""
                )

                (report.winners + report.losers)
                    .map { it to positionUpdates[it] }
                    .filter { it.second != null }
                    .forEach {
                        it.second!!.thenAcceptAsync { updates ->
                            val player = Bukkit.getPlayer(it.first)
                                ?: return@thenAcceptAsync

                            player.sendMessage("${CC.PRI}Leaderboards:")
                            player.sendMessage("${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.SEC}Update: ${
                                if (updates.newPosition < updates.oldPosition) CC.RED else CC.GREEN
                            }${
                                updates.newPosition - updates.oldPosition
                            } ${CC.GRAY}(#${
                                Numbers.format(updates.oldPosition)
                            } ${Constants.ARROW_RIGHT} ${
                                Numbers.format(updates.newPosition)
                            })")

                            if (updates.nextPosition == null)
                            {
                                player.sendMessage("${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.GREEN}You are #1 on the leaderboards!")
                                return@thenAcceptAsync
                            }

                            player.sendMessage(
                                "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                    "${CC.SEC}You need ${CC.PRI}${
                                        updates.requiredScore()
                                    }${CC.SEC} ELO to reach ${CC.GREEN}#${
                                        Numbers.format(updates.nextPosition!!.position)
                                    } ${CC.GRAY}(${
                                        updates.nextPosition!!.uniqueId.username()
                                    })${CC.SEC}."
                                }"
                            )
                        }
                    }
            }

            this.game.audiencesIndexed { audience, player ->
                audience.sendTitlePart(
                    TitlePart.TITLE,
                    Component
                        .text(
                            if (player in this.report.winners)
                                "VICTORY!" else "DEFEAT!"
                        )
                        .color(
                            if (player in this.report.winners)
                                NamedTextColor.GREEN else NamedTextColor.RED
                        )
                        .decorate(TextDecoration.BOLD)
                )
            }
        }

        if (this.game.activeCountdown == 5)
        {
            this.game.sendMessage("${CC.GRAY}You will be sent to a lobby in 5 seconds.")
        }

        if (this.game.activeCountdown <= 0)
        {
            this.game.closeAndCleanup()
            return
        }

        this.game.activeCountdown--
    }
}
