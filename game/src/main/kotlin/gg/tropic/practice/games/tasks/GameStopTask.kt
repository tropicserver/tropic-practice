package gg.tropic.practice.games.tasks

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.TitlePart
import java.util.UUID

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class GameStopTask(
    private val game: GameImpl,
    private val report: GameReport,
    private val eloMappings: Map<UUID, Pair<Int, Int>>
) : Runnable
{
    lateinit var task: Task

    override fun run()
    {
        if (this.game.activeCountdown == 5)
        {
            this.game.sendMessage(
                "",
                "${CC.PRI}Match Overview:",
                "${CC.I_GRAY}(Click to view inventories)",
                "",
                "${CC.GREEN}Winner${
                    if (this.report.winners.size == 1) "" else "s"
                }: ${CC.WHITE}${
                    if (this.report.winners.isEmpty()) "N/A" else this
                        .report.winners.joinToString(", ") {
                            it.username()
                        }
                }",
                "${CC.RED}Loser${
                    if (this.report.losers.size == 1) "" else "s"
                }: ${CC.WHITE}${
                    if (this.report.losers.isEmpty()) "N/A" else this
                        .report.losers.joinToString(", ") {
                            it.username()
                        }
                }",
                ""
            )

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
            this.game.closeAndCleanup(
                "${CC.SEC}${
                    if (this.report.winners.isEmpty()) "N/A" else this
                        .report.winners.joinToString(", ") {
                            it.username()
                        }
                }${CC.GREEN} won the game!"
            )

            this.task.closeAndReportException()
            return
        }

        this.game.activeCountdown--
    }
}
