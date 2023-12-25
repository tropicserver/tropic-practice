package gg.tropic.practice.menu.tournaments

import gg.scala.flavor.service.Service
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.region.Region
import gg.tropic.practice.services.TournamentManagerService
import gg.tropic.practice.tournaments.TournamentConfig
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.buttons.TexturedHeadButton
import net.evilblock.cubed.menu.menus.ConfirmMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.prompt.NumberPrompt
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@Service
object TournamentCreationPipeline
{
    private fun stage6Create(
        creator: Player,
        teamSize: Int,
        maxSize: Int,
        kit: Kit,
        region: Region
    )
    {
        val tournamentConfig = TournamentConfig(
            teamSize = teamSize,
            maxPlayers = maxSize,
            kitID = kit.id,
            region = region,
            creator = creator.uniqueId,
            creatorBypassesCreationRequirements = creator
                .hasPermission("practice.tournament.bypass-creation-requirements")
        )

        creator.sendMessage("${CC.GRAY}Attempting to start the tournament...")
        TournamentManagerService
            .publish(
                id = "create",
                "config" to tournamentConfig
            )
    }

    private fun stage5ConfirmCreation(
        creator: Player,
        teamSize: Int,
        maxSize: Int,
        kit: Kit,
        region: Region
    ) = ConfirmMenu(
        title = "Confirm tournament!",
        extraInfo = listOf(
            "${CC.GRAY}Team Size: ${CC.GREEN}$teamSize",
            "${CC.GRAY}Tournament Size: ${CC.GREEN}$maxSize",
            "${CC.GRAY}Kit: ${CC.GREEN}${kit.displayName}",
            "${CC.GRAY}Region: ${CC.GREEN}${region.name}"
        ),
        confirm = true
    ) { confirmed ->
        if (!confirmed)
        {
            creator.sendMessage("${CC.RED}You have cancelled the creation of the tournament!")
            return@ConfirmMenu
        }

        stage6Create(creator, teamSize, maxSize, kit, region)
    }

    private fun stage4SelectRegion(teamSize: Int, maxSize: Int, kit: Kit) =
        object : Menu("Select a region...")
        {
            init
            {
                placeholder = true
            }

            override fun size(buttons: Map<Int, Button>) = 27
            override fun getButtons(player: Player) = Region.entries
                .mapIndexed { index, region ->
                    10 + index to ItemBuilder
                        .copyOf(
                            object : TexturedHeadButton(Constants.GLOBE_ICON)
                            {}
                                .getButtonItem(player)
                        )
                        .name("${CC.B_GREEN}${region.name}")
                        .toButton { _, _ ->
                            stage5ConfirmCreation(player, teamSize, maxSize, kit, region)
                                .openMenu(player)
                        }
                }
                .associate { it }
        }

    private fun stage3SelectKit(teamSize: Int, maxSize: Int) =
        object : TemplateKitMenu()
        {
            override fun shouldIncludeKitDescription() = false
            override fun filterDisplayOfKit(player: Player, kit: Kit) = kit
                .queueSizes.any { it.first == teamSize }

            override fun itemTitleFor(player: Player, kit: Kit) = "${CC.B_GREEN}${kit.displayName}"
            override fun itemDescriptionOf(player: Player, kit: Kit) = listOf(
                "",
                "${CC.GREEN}Click to select!"
            )

            override fun itemClicked(player: Player, kit: Kit, type: ClickType)
            {
                stage4SelectRegion(teamSize, maxSize, kit).openMenu(player)
            }

            override fun getPrePaginatedTitle(player: Player) = "Select a kit..."
        }

    private fun stage2SelectMaxSize(player: Player, teamSize: Int) = NumberPrompt()
        .withText("${CC.D_GREEN}Enter a tournament player size ${CC.GRAY}(\"cancel\" to cancel)${CC.D_GREEN}:")
        .acceptInput {
            val matchSize = teamSize * 2
            val maxSizeAppliesToTeamSize =
                it.toInt().mod(matchSize) == 0

            if (!maxSizeAppliesToTeamSize)
            {
                player.sendMessage("${CC.RED}Please enter an even number for your tournament player size!")
                return@acceptInput
            }

            player.sendMessage("${CC.GREEN}You selected: ${CC.WHITE}$it")
            stage3SelectKit(teamSize, it.toInt()).openMenu(player)
        }

    fun start() = object : Menu(
        "Select a team size..."
    )
    {
        init
        {
            placeholder = true
        }

        private fun teamSizeOf(size: Int) = ItemBuilder
            .of(Material.PAPER)
            .name("${CC.PRI}${size}v${size}")
            .toButton { player, _ ->
                player!!.closeInventory()
                stage2SelectMaxSize(player, size).start(player)
            }

        override fun size(buttons: Map<Int, Button>) = 27
        override fun getButtons(player: Player) = mutableMapOf(
            10 to teamSizeOf(1),
            11 to teamSizeOf(2),
            12 to teamSizeOf(3)
        )
    }
}
