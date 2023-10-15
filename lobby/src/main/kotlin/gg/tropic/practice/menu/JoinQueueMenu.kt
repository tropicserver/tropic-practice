package gg.tropic.practice.menu

import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.queue.QueueService
import net.evilblock.cubed.util.CC
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
    override fun filterDisplayOfKit(player: Player, kit: Kit) = true
    override fun itemDescriptionOf(player: Player, kit: Kit) = listOf("${CC.YELLOW}Click to join!")

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        val lobbyPlayer = LobbyPlayerService
            .find(player.uniqueId)
            ?: return

        player.closeInventory()

        QueueService.joinQueue(kit, queueType, teamSize, player)
        lobbyPlayer.state = PlayerState.InQueue

        player.playSound(player.location, Sound.NOTE_PLING, 1f, 1f)
        player.sendMessage(
            "${CC.GREEN}You have joined the ${CC.PRI}${queueType.name} ${kit.displayName}${CC.GREEN} queue!"
        )
    }

    override fun getPrePaginatedTitle(player: Player) =
        "Joining a ${queueType.name} ${teamSize}v$teamSize queue..."
}
