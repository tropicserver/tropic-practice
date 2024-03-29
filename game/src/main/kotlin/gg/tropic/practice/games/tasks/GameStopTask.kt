package gg.tropic.practice.games.tasks

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameReport
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.leaderboards.ScoreUpdates
import gg.tropic.practice.serializable.Message
import gg.tropic.practice.settings.isASilentSpectator
import me.lucko.helper.Events
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.math.Numbers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class GameStopTask(
    private val game: GameImpl,
    private val report: GameReport,
    private val eloMappings: Map<UUID, Pair<Int, Int>>,
    private val positionUpdates: Map<UUID, CompletableFuture<ScoreUpdates>>,
    private val terminationReason: String,
    private val playerFeedback: Map<UUID, MutableList<String>>
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
                " ${CC.PRI}Match Overview ${CC.I_GRAY}(Click to view inventories)",
            )

            fun Message.appendPlayers(players: List<UUID>)
            {
                if (players.isEmpty())
                {
                    return
                }

                for ((index, winner) in players.withIndex())
                {
                    withMessage(CC.YELLOW + winner.username())
                        .andHoverOf(
                            "${CC.GREEN}Click to view inventory!"
                        )
                        .andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/matchinventory ${report.identifier} $winner"
                        )

                    if (index < players.size - 1)
                    {
                        withMessage(", ")
                    }
                }
            }

            val matchIs1v1 = report.losers.size == 1 &&
                report.winners.size == 1

            val winnerComponent = Message()
                .withMessage(
                    " ${CC.GREEN}Winner${
                        if (this.report.winners.size == 1) "" else "s"
                    }: ${CC.WHITE}${
                        if (report.winners.isEmpty()) "${CC.WHITE}None!" else ""
                    }"
                )

            val loserComponent = Message()
                .withMessage(
                    "${if (!matchIs1v1) " " else ""}${CC.RED}Loser${
                        if (this.report.losers.size == 1) "" else "s"
                    }: ${CC.WHITE}${
                        if (report.losers.isEmpty()) "${CC.WHITE}None!" else ""
                    }"
                )

            winnerComponent.appendPlayers(report.winners)
            loserComponent.appendPlayers(report.losers)

            if (matchIs1v1)
            {
                val consolidatedMessage = Message()
                consolidatedMessage.components += winnerComponent.components
                consolidatedMessage.withMessage(" ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ")
                consolidatedMessage.components += loserComponent.components
                consolidatedMessage.consolidate()

                game.sendMessage(consolidatedMessage)
            } else
            {
                val consolidatedMessage = Message()
                consolidatedMessage.components += winnerComponent.components
                consolidatedMessage.consolidate()
                game.sendMessage(consolidatedMessage)

                val secondMessage = Message()
                secondMessage.components += loserComponent.components
                secondMessage.consolidate()
                game.sendMessage(secondMessage)
            }

            this.game.sendMessage("")

            val spectators = game.expectedSpectators
                .mapNotNull(Bukkit::getPlayer)
                .filter {
                    !it.hasMetadata("vanished") && !it.isASilentSpectator()
                }

            if (spectators.isNotEmpty())
            {
                game.sendMessage(
                    " ${CC.YELLOW}Spectators ${CC.GRAY}(${
                        spectators.size
                    })${CC.YELLOW}: ${CC.WHITE}${
                        spectators.take(3)
                            .joinToString(
                                separator = ", ",
                                transform = Player::getName
                            )
                    }${
                        if (spectators.size > 3) " ${CC.GRAY}(and ${
                            spectators.size - 3
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
                    " ${CC.PINK}ELO Updates:",
                    " ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.GREEN}${winner.username()}:${CC.WHITE} ${eloMappings[winner]!!.first} ${CC.GRAY}(${CC.GREEN}+${eloMappings[winner]!!.second}${CC.GRAY})",
                    " ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.RED}${loser.username()}:${CC.WHITE} ${eloMappings[loser]!!.first} ${CC.GRAY}(${CC.RED}${eloMappings[loser]!!.second}${CC.GRAY})",
                    ""
                )

                (report.winners + report.losers)
                    .map { it to positionUpdates[it] }
                    .filter { it.second != null }
                    .forEach {
                        it.second!!.thenAcceptAsync { updates ->
                            val player = Bukkit.getPlayer(it.first)
                                ?: return@thenAcceptAsync

                            player.sendMessage(" ${CC.PRI}Leaderboards:")
                            player.sendMessage(
                                " ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.SEC}Moved: ${
                                    when (true)
                                    {
                                        (updates.newPosition == updates.oldPosition) -> CC.GRAY
                                        (updates.newPosition < updates.oldPosition) -> "${CC.GREEN}${CC.BOLD}▲${CC.GREEN}"
                                        else -> "${CC.RED}${CC.BOLD}▼${CC.RED}"
                                    }
                                }${
                                    abs(updates.newPosition - updates.oldPosition)
                                } ${CC.GRAY}(#${
                                    Numbers.format(updates.oldPosition + 1)
                                } ${Constants.ARROW_RIGHT}${CC.R}${CC.GRAY} #${
                                    Numbers.format(updates.newPosition + 1)
                                })"
                            )

                            if (updates.nextPosition == null)
                            {
                                player.sendMessage(" ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.GREEN}You are #1 on the leaderboards!")
                                player.sendMessage("")
                                return@thenAcceptAsync
                            }

                            player.sendMessage(
                                " ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                    "${CC.SEC}You need ${CC.PRI}${
                                        updates.requiredScore()
                                    }${CC.SEC} ELO to reach ${CC.GREEN}#${
                                        Numbers.format(
                                            updates.nextPosition!!.position + 1
                                        )
                                    } ${CC.GRAY}(${
                                        updates.nextPosition!!.uniqueId.username()
                                    })${CC.SEC}."
                                }"
                            )
                            player.sendMessage("")
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

            if (terminationReason.isNotBlank())
            {
                game.sendMessage(
                    "${CC.B_RED}✗ ${CC.RED}Your match was terminated!",
                    "${CC.RED}Reason: ${CC.WHITE}$terminationReason",
                    ""
                )
            }

            Tasks.delayed(10L) {
                playerFeedback.forEach { (user, feedback) ->
                    val player = Bukkit.getPlayer(user)
                        ?: return@forEach

                    if (feedback.isNotEmpty())
                    {
                        feedback.forEach(player::sendMessage)
                    }
                }
            }
        }

        if (this.game.activeCountdown == 4 && game.expectationModel.queueId != "tournament" && game.expectationModel.queueId != "party")
        {
            if (game.expectationModel.queueId != null)
            {
                val reQueueItem = ItemBuilder
                    .of(Material.PAPER)
                    .name("${CC.GREEN}Click to join queue ${CC.GRAY}(Right-Click)")
                    .build()

                game.toBukkitPlayers()
                    .filterNotNull()
                    .forEach {
                        it.itemInHand = reQueueItem
                        it.updateInventory()
                    }

                Events
                    .subscribe(PlayerInteractEvent::class.java)
                    .filter {
                        it.action.name.contains("RIGHT") &&
                            it.hasItem() && it.item.isSimilar(reQueueItem)
                    }
                    .handler {
                        it.player.itemInHand = ItemStack(Material.AIR)
                        it.player.updateInventory()
                        it.player.sendMessage(
                            "${CC.GREEN}You will be queued again when you return to spawn!"
                        )

                        game.expectedQueueRejoin += it.player.uniqueId
                        Button.playNeutral(it.player)
                    }
                    .bindWith(game)
            }
        }

        if (this.game.activeCountdown <= 0)
        {
            this.game.closeAndCleanup()
            return
        }

        this.game.activeCountdown--
    }
}
