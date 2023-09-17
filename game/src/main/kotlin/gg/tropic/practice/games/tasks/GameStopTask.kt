package gg.tropic.practice.games.tasks

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.TitlePart

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class GameStopTask(
    private val game: GameImpl,
    private val report: GameReport
) : Runnable
{
    lateinit var task: Task

    override fun run()
    {
        if (this.game.activeCountdown == 5)
        {
            this.game.sendMessage(
                "",
                "  ${CC.B_AQUA}GAME OVERVIEW:",
                "  ${CC.GRAY}Duration: ${CC.WHITE}${this.game.getDuration()}",
                "",
                "  ${CC.GREEN}Winner${
                    if (this.report.winners.size == 1) "" else "s"
                }: ${CC.WHITE}${
                    if (this.report.winners.isEmpty()) "N/A" else this
                        .report.winners.joinToString(", ") {
                            it.username()
                        }
                }",
                "  ${CC.RED}Loser${
                    if (this.report.losers.size == 1) "" else "s"
                }: ${CC.WHITE}${
                    if (this.report.losers.isEmpty()) "N/A" else this
                        .report.losers.joinToString(", ") {
                            it.username()
                        }
                }",
                ""
            )

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

        when (this.game.activeCountdown)
        {
            5, 4, 3, 2, 1 ->
            {
                this.game.sendMessage("${CC.RED}You will be sent to a hub in ${this.game.activeCountdown}...")
            }
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
