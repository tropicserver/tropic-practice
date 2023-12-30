package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.command.ScalaCommandManager
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.hologram.ScrollingKitLeaderboardHologram
import gg.tropic.practice.hologram.ScrollingTypeLeaderboardHologram
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 12/29/2023
 */
@AutoRegister
@CommandAlias("spawnhologram")
@CommandPermission("practice.command.spawnhologram")
object SpawnHologramCommand : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandCompletions.registerStaticCompletion(
            "leaderboard-types",
            ReferenceLeaderboardType.entries
                .map(ReferenceLeaderboardType::name)
        )
    }

    @AssignPermission
    @Subcommand("scrolling-leaderboardtypes")
    @CommandCompletion("* @leaderboard-types @kits")
    @Description("Spawn a hologram that scrolls through a list of leaderboard types.")
    fun onScrollingLBTypes(
        player: ScalaPlayer, delay: Int,
        @Single leaderboardTypes: String,
        @Optional kit: Kit?
    )
    {
        val hologram = ScrollingTypeLeaderboardHologram(
            scrollStates = leaderboardTypes.split(","),
            scrollTime = delay,
            location = player.bukkit().location,
            kitID = kit?.id
        )

        hologram.configure()
        player.sendMessage("${CC.GREEN}Spawned the hologram!")
    }

    @AssignPermission
    @Subcommand("scrolling-kits")
    @CommandCompletion("* @kits @leaderboard-types")
    @Description("Spawn a hologram that scrolls through a list of kits.")
    fun onScrollingKits(
        player: ScalaPlayer, delay: Int,
        @Single kits: String,
        leaderboardType: ReferenceLeaderboardType
    )
    {
        val hologram = ScrollingKitLeaderboardHologram(
            kits = kits.split(","),
            scrollTime = delay,
            location = player.bukkit().location,
            scrollState = leaderboardType.name
        )

        hologram.configure()
        player.sendMessage("${CC.GREEN}Spawned the hologram!")
    }
}
