package gg.tropic.practice.menu

import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.services.GameManagerService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
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

    override fun itemTitleFor(player: Player, kit: Kit) = "${CC.B_PRI}${kit.displayName}${
        if (kit.features(FeatureFlag.NewlyCreated)) " ${CC.B_YELLOW}NEW!" else ""
    }"

    override fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    {
        val queueId = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"
        val metadata = GameManagerService.buildQueueIdMetadataTracker(queueId)

        return listOf(
            "${CC.WHITE}Playing: ${CC.PRI}${metadata.inGame}",
            "${CC.WHITE}Queueing: ${CC.PRI}${metadata.inQueue}",
            "",
            "${CC.GREEN}Click to queue!"
        )
    }

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

        Button.playNeutral(player)
        player.sendMessage(
            "${CC.GREEN}You have joined the ${CC.PRI}${queueType.name} ${teamSize}v$teamSize ${kit.displayName}${CC.GREEN} queue!"
        )
    }

    override fun getPrePaginatedTitle(player: Player) =
        "Queueing ${queueType.name} ${teamSize}v$teamSize..."
}
