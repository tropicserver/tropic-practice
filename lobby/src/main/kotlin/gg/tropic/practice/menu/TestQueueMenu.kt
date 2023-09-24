package gg.tropic.practice.menu

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.queue.QueueService
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
class TestQueueMenu : TemplateKitMenu()
{
    override fun filterDisplayOfKit(player: Player, kit: Kit) = true
    override fun itemDescriptionOf(player: Player, kit: Kit) = listOf("click to join queue")

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        QueueService.joinQueue(kit, player)
    }

    override fun getPrePaginatedTitle(player: Player) = "Test Queue pre-production"
}
