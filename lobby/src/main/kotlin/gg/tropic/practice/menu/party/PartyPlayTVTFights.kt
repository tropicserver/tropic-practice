package gg.tropic.practice.menu.party

import com.cryptomorin.xseries.XMaterial
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.menu.template.TemplateMapMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import gg.tropic.practice.region.Region
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.buttons.TexturedHeadButton
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.*

/**
 * @author GrowlyX
 * @since 2/9/2024
 */
class PartyPlayTVTFights(private val onlinePlayerList: List<UUID>) : PaginatedMenu()
{
    init
    {
        placeholdBorders = true
        updateAfterClick = true
    }

    override fun size(buttons: Map<Int, Button>) = 36
    override fun getAllPagesButtonSlots() = (10..16) + (19..25)

    private val selectedPlayerList = mutableListOf<UUID>()
    override fun getAllPagesButtons(player: Player) = onlinePlayerList
        .mapIndexed { index, uuid ->
            index to ItemBuilder
                .of(
                    if (uuid in selectedPlayerList)
                        XMaterial.LIME_WOOL else XMaterial.PLAYER_HEAD
                )
                .apply {
                    if (uuid !in selectedPlayerList)
                        owner(uuid.username())
                }
                .name(
                    "${
                        if (uuid in selectedPlayerList)
                            CC.B_GREEN else CC.GREEN
                    }${uuid.username()}"
                )
                .toButton { _, _ ->
                    if (uuid in selectedPlayerList)
                    {
                        Button.playNeutral(player)
                        selectedPlayerList -= uuid
                    } else
                    {
                        if (selectedPlayerList.size >= onlinePlayerList.size - 1)
                        {
                            Button.playFail(player)
                            player.sendMessage("${CC.RED}You must leave at least one player for the other team!")
                            return@toButton
                        }

                        Button.playNeutral(player)
                        selectedPlayerList += uuid
                    }
                }
        }
        .associate { it }

    override fun getGlobalButtons(player: Player) = mapOf(
        4 to ItemBuilder
            .of(XMaterial.LIME_STAINED_GLASS_PANE)
            .name("${CC.GREEN}Continue")
            .addToLore(
                "${CC.GRAY}Team 1:"
            )
            .apply {
                if (selectedPlayerList.isEmpty())
                {
                    addToLore("${CC.PINK}Random!")
                    return@apply
                }

                selectedPlayerList.forEach {
                    addToLore("${CC.WHITE}- ${it.username()}")
                }
            }
            .addToLore("", "${CC.GRAY}Team 2:")
            .apply {
                val team2 = if (selectedPlayerList.isEmpty())
                    emptyList() else onlinePlayerList.filter { it !in selectedPlayerList }

                if (team2.isEmpty())
                {
                    addToLore("${CC.PINK}Random!")
                    return@apply
                }

                team2.forEach {
                    addToLore("${CC.WHITE}- ${it.username()}")
                }
            }
            .toButton { _, _ ->
                var team1 = if (selectedPlayerList.isEmpty())
                    emptyList() else selectedPlayerList.toList()

                var team2 = if (selectedPlayerList.isEmpty())
                    emptyList() else onlinePlayerList
                    .filter { it !in selectedPlayerList }

                if (team1.isEmpty() || team2.isEmpty())
                {
                    val twoTeams = onlinePlayerList.shuffled()
                        .chunked(onlinePlayerList.size / 2)
                    team1 = twoTeams[0]
                    team2 = twoTeams[1]
                }

                stage2PartyPlay(team1, team2).openMenu(player)
            }
    )

    private fun stage2PartyPlay(team1: List<UUID>, team2: List<UUID>) = object : TemplateKitMenu()
    {
        private var regionSelection: Region? = null

        init
        {
            updateAfterClick = true
        }

        override fun getGlobalButtons(player: Player) = mapOf(
            4 to ItemBuilder
                .copyOf(
                    object : TexturedHeadButton(Constants.GLOBE_ICON)
                    {}.getButtonItem(player)
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

        override fun getPrePaginatedTitle(player: Player) = "Select a kit..."
        override fun filterDisplayOfKit(player: Player, kit: Kit) = true

        override fun itemTitleFor(player: Player, kit: Kit) = "${CC.B_GREEN}${kit.displayName}"
        override fun itemDescriptionOf(player: Player, kit: Kit) = listOf(
            "",
            "${CC.GREEN}Click to select!"
        )

        override fun shouldIncludeKitDescription() = false

        override fun itemClicked(player: Player, kit: Kit, type: ClickType)
        {
            val mapSelectionStage = object : TemplateMapMenu()
            {
                override fun filterDisplayOfMap(map: gg.tropic.practice.map.Map) = map.associatedKitGroups
                    .intersect(
                        KitGroupService.groupsOf(kit)
                            .map(KitGroup::id)
                            .toSet()
                    )
                    .isNotEmpty()

                override fun itemTitleFor(player: Player, map: gg.tropic.practice.map.Map) =
                    "${CC.B_GREEN}${map.displayName}"

                override fun itemDescriptionOf(player: Player, map: gg.tropic.practice.map.Map) = listOf(
                    "",
                    "${CC.GREEN}Click to select!"
                )

                override fun itemClicked(player: Player, map: gg.tropic.practice.map.Map, type: ClickType)
                {
                    Button.playNeutral(player)
                    startMatchWithMapKitCombo(player, kit, map)
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
                            startMatchWithMapKitCombo(player, kit, getAvailableMaps().random())
                        }
                )

                override fun getPrePaginatedTitle(player: Player) = "Select a map..."
            }

            if (player.hasPermission("practice.party.play.select-custom-map"))
            {
                if (!mapSelectionStage.ensureMapsAvailable())
                {
                    Button.playFail(player)
                    player.sendMessage("${CC.RED}There are no maps associated with the kit ${CC.YELLOW}${kit.displayName}${CC.RED}!")
                    return
                }

                Button.playNeutral(player)
                mapSelectionStage.openMenu(player)
                return
            }

            Button.playNeutral(player)
            startMatchWithMapKitCombo(
                player, kit, mapSelectionStage.getAvailableMaps().random()
            )
        }

        fun startMatchWithMapKitCombo(
            player: Player, kit: Kit, map: gg.tropic.practice.map.Map
        )
        {
            val gameExpectation = GameExpectation(
                UUID.randomUUID(),
                players = team1 + team2,
                teams = mapOf(
                    GameTeamSide.A to GameTeam(GameTeamSide.A, team1),
                    GameTeamSide.B to GameTeam(GameTeamSide.B, team2)
                ),
                kitId = kit.id,
                mapId = map.name,
                queueId = "party"
            )

            player.closeInventory()
            val lobbyPlayer = LobbyPlayerService.find(player)
                ?: return run {
                    player.sendMessage("${CC.RED}You are unable to do this right now!")
                }

            if (!lobbyPlayer.isInParty())
            {
                player.sendMessage("${CC.RED}You are no longer in a party!")
                return
            }

            lobbyPlayer.partyOf().onlinePracticePlayersInLobby()
                .thenAccept {
                    if (!gameExpectation.players.all { player -> player in it.keys })
                    {
                        player.sendMessage("${CC.RED}One or more of the party members you selected are unavailable to play in this TvT game.")
                        return@thenAccept
                    }

                    val playerRegion = PlayerRegionFromRedisProxy
                        .of(player)
                        .join()

                    QueueService.createMessage(
                        "create-match",
                        "config" to gameExpectation,
                        "map" to map.name,
                        "kit" to kit.id,
                        "region" to (regionSelection ?: playerRegion).name
                    ).publish(
                        channel = "practice:queue".suffixWhenDev()
                    )

                    Button.playSuccess(player)
                    player.sendMessage("${CC.B_GREEN}We're preparing your match...")
                }
        }
    }

    override fun getPrePaginatedTitle(player: Player) = "Selecting team 1..."
}
