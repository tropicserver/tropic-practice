package gg.tropic.practice.menu.pipeline

import gg.scala.aware.thread.AwareThreadContext
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.duel.DuelRequestUtilities
import gg.tropic.practice.games.duels.DuelRequest
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import gg.tropic.practice.map.Map
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.menu.template.TemplateMapMenu
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import gg.tropic.practice.region.Region
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.buttons.TexturedHeadButton
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.nms.MinecraftReflection
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.*

/**
 * @author GrowlyX
 * @since 10/21/2023
 */
object DuelRequestPipeline
{
    private fun stage2ASendDuelRequestRandomMap(
        player: Player,
        target: UUID,
        kit: Kit,
        selectedRegion: Region?
    )
    {
        player.closeInventory()

        PlayerRegionFromRedisProxy.of(player)
            .thenApplyAsync {
                val request = DuelRequest(
                    requester = player.uniqueId,
                    requesterPing = MinecraftReflection.getPing(player),
                    requestee = target,
                    region = selectedRegion ?: it,
                    kitID = kit.id
                )

                stage4PublishDuelRequest(request)
                it
            }
            .whenComplete { region, throwable ->
                if (throwable != null)
                {
                    player.sendMessage("${CC.RED}We were unable to send the duel request!")
                    return@whenComplete
                }

                player.sendMessage("Sent ${CC.PRI}${target.username()} ${CC.SEC}a ${CC.GREEN}${kit.displayName}${CC.SEC} duel request on a random map. ${CC.GRAY}(${(selectedRegion ?: region).name} Region)")
            }
    }

    private fun stage3SendDuelRequest(
        player: Player,
        target: UUID,
        kit: Kit,
        map: Map,
        selectedRegion: Region?
    )
    {
        player.closeInventory()

        PlayerRegionFromRedisProxy.of(player)
            .thenApply {
                val request = DuelRequest(
                    requester = player.uniqueId,
                    requesterPing = MinecraftReflection.getPing(player),
                    requestee = target,
                    region = selectedRegion ?: it,
                    kitID = kit.id,
                    mapID = map.name
                )

                stage4PublishDuelRequest(request)
                it
            }
            .whenComplete { region, throwable ->
                if (throwable != null)
                {
                    player.sendMessage("${CC.RED}We were unable to send the duel request!")
                    return@whenComplete
                }

                player.sendMessage("Sent ${CC.PRI}${target.username()} ${CC.SEC}a ${CC.GREEN}${kit.displayName}${CC.SEC} duel request on ${CC.GREEN}${map.displayName}${CC.SEC}. ${CC.GRAY}(${(selectedRegion ?: region).name} Region)")
            }
    }

    private fun stage4PublishDuelRequest(request: DuelRequest)
    {
        QueueService.createMessage(
            "request-duel",
            "request" to request
        ).publish(
            channel = "practice:queue",
            context = AwareThreadContext.SYNC
        )
    }

    fun automateDuelRequestNoUI(player: Player, target: UUID, kit: Kit, map: Map, region: Region)
    {
        if (player.hasPermission("practice.duel.select-custom-map"))
        {
            stage3SendDuelRequest(
                target = target, kit = kit,
                player = player, map = map,
                selectedRegion = region
            )
        } else
        {
            stage2ASendDuelRequestRandomMap(
                player = player, target = target,
                kit = kit,
                selectedRegion = region
            )
        }
    }

    private fun stage2BSendDuelRequestCustomMap(
        target: UUID,
        kit: Kit,
        previous: Menu,
        // weird initialization issues from parent/super class requires us to pass this through
        kitGroups: Set<String> = KitGroupService.groupsOf(kit)
            .map(KitGroup::id)
            .toSet(),
        selectedRegion: Region?
    ) = object : TemplateMapMenu()
    {
        override fun filterDisplayOfMap(map: Map) = map.associatedKitGroups
            .intersect(kitGroups)
            .isNotEmpty()

        override fun itemTitleFor(player: Player, map: Map) = "${CC.B_GREEN}${map.displayName}"
        override fun itemDescriptionOf(player: Player, map: Map) = listOf(
            "",
            "${CC.GREEN}Click to select!"
        )

        override fun itemClicked(player: Player, map: Map, type: ClickType)
        {
            Button.playNeutral(player)
            stage3SendDuelRequest(player, target, kit, map, selectedRegion)
        }

        override fun getGlobalButtons(player: Player) = mutableMapOf(
            4 to ItemBuilder
                .of(Material.NETHER_STAR)
                .name("${CC.B_AQUA}Random Map")
                .addToLore(
                    "",
                    "${CC.AQUA}Click to select!"
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    stage2ASendDuelRequestRandomMap(player, target, kit, selectedRegion)
                }
        )

        override fun getPrePaginatedTitle(player: Player) = "Select a map..."

        override fun onClose(player: Player, manualClose: Boolean)
        {
            if (manualClose)
            {
                Tasks.sync { previous.openMenu(player) }
            }
        }
    }

    fun build(target: UUID) = object : TemplateKitMenu()
    {
        private var kitSelectionLock = false
        private var regionSelection: Region? = null

        init
        {
            updateAfterClick = true
        }

        override fun getGlobalButtons(player: Player) = mapOf(
            4 to ItemBuilder
                .copyOf(
                    object : TexturedHeadButton(Constants.GLOBE_ICON){}.getButtonItem(player)
                )
                .name("${CC.GREEN}Region")
                .addToLore(
                    "${
                        if (regionSelection == null) CC.B_WHITE else CC.GRAY
                    }Closest ${CC.D_GRAY}${
                        Constants.THIN_VERTICAL_LINE
                    } ${
                        if (regionSelection == Region.NA) CC.B_WHITE else CC.GRAY
                    }NA ${CC.D_GRAY}${
                        Constants.THIN_VERTICAL_LINE
                    } ${
                        if (regionSelection == Region.EU) CC.B_WHITE else CC.GRAY
                    }EU"
                )
                .toButton { _, _ ->
                    val oldRegionSelection = regionSelection
                    regionSelection = when (oldRegionSelection)
                    {
                        null -> Region.NA
                        Region.NA -> Region.EU
                        Region.EU -> null
                        Region.Both -> null
                    }

                    Button.playNeutral(player)
                }
        )

        override fun filterDisplayOfKit(player: Player, kit: Kit) = true

        override fun itemTitleFor(player: Player, kit: Kit) = "${CC.B_GREEN}${kit.displayName}"
        override fun itemDescriptionOf(player: Player, kit: Kit) = listOf(
            "",
            "${CC.GREEN}Click to select!"
        )

        override fun shouldIncludeKitDescription() = false

        override fun itemClicked(player: Player, kit: Kit, type: ClickType)
        {
            if (kitSelectionLock)
            {
                return
            }

            kitSelectionLock = true

            DuelRequestUtilities
                .duelRequestExists(player.uniqueId, target, kit)
                .thenAccept {
                    if (it)
                    {
                        kitSelectionLock = false
                        player.sendMessage(
                            "${CC.RED}You already have an outgoing duel request to ${target.username()} with kit ${kit.displayName}!"
                        )
                        return@thenAccept
                    }

                    if (player.hasPermission("practice.duel.select-custom-map"))
                    {
                        val stage2B = stage2BSendDuelRequestCustomMap(
                            target, kit, this, selectedRegion = regionSelection
                        )

                        if (!stage2B.ensureMapsAvailable())
                        {
                            kitSelectionLock = false

                            Button.playFail(player)
                            player.sendMessage("${CC.RED}There are no maps associated with the kit ${CC.YELLOW}${kit.displayName}${CC.RED}!")
                            return@thenAccept
                        }

                        kitSelectionLock = false

                        Button.playNeutral(player)
                        stage2B.openMenu(player)
                        return@thenAccept
                    }

                    kitSelectionLock = false

                    Button.playNeutral(player)
                    stage2ASendDuelRequestRandomMap(player, target, kit,
                        regionSelection)
                }
                .exceptionally {
                    kitSelectionLock = false

                    it.printStackTrace()
                    return@exceptionally null
                }
        }

        override fun getPrePaginatedTitle(player: Player) = "Select a kit..."
    }
}
