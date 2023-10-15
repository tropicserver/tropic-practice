package gg.tropic.practice.menu

import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.queue.QueueService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
class JoinQueueMenu(
    private val queueType: QueueType,
    private val teamSize: Int
) : TemplateKitMenu()
{
    override fun filterDisplayOfKit(player: Player, kit: Kit) = kit.queueSizes
        .any {
            it.first == teamSize && queueType in it.second
        }

    override fun itemDescriptionOf(player: Player, kit: Kit) = listOf(
        "${CC.WHITE}Playing: ${CC.PRI}0",
        "${CC.WHITE}Queued: ${CC.PRI}0",
        "",
        "${CC.B_GREEN}Daily Win Streaks:",
        "${CC.GREEN}#1 ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}GrowlyX ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}0",
        "${CC.GREEN}#2 ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}GrowlyX ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}0",
        "${CC.GREEN}#3 ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}GrowlyX ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}0",
        "",
        "${CC.GREEN}Click to join!"
    )

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        val lobbyPlayer = LobbyPlayerService
            .find(player.uniqueId)
            ?: return

        if (lobbyPlayer.state == PlayerState.InQueue)
        {
            player.sendMessage("${CC.RED}You are already in a queue!")
            return
        }

        player.closeInventory()
        QueueService.joinQueue(kit, queueType, teamSize, player)

        synchronized(lobbyPlayer.stateUpdateLock) {
            lobbyPlayer.state = PlayerState.InQueue
        }

        player.playSound(player.location, Sound.NOTE_PLING, 1f, 1f)
        player.sendMessage(
            "${CC.GREEN}You have joined the ${CC.PRI}${queueType.name} ${teamSize}v$teamSize ${kit.displayName}${CC.GREEN} queue!"
        )
    }

    override fun getPrePaginatedTitle(player: Player) =
        "Joining a ${queueType.name} ${teamSize}v$teamSize queue..."
}
