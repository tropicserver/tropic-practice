package gg.tropic.practice.menu

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.services.LeaderboardManagerService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.buttons.TexturedHeadButton
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
class LeaderboardsMenu : TemplateKitMenu()
{
    private var menuState = ReferenceLeaderboardType.ELO
    override fun filterDisplayOfKit(player: Player, kit: Kit) =
        !menuState.enforceRanked || kit.features(FeatureFlag.Ranked)

    init
    {
        async = true
        updateAfterClick = true
    }

    private val scoreCache = mutableMapOf<Reference, Pair<Long?, Long?>>()

    override fun asyncLoadResources(player: Player, callback: (Boolean) -> Unit)
    {
        callback(true)
    }

    override fun itemTitleFor(player: Player, kit: Kit) = "${CC.PRI}${kit.displayName}"
    override fun shouldIncludeKitDescription() = false

    private fun localDescriptionOf(player: Player, reference: Reference): List<String>
    {
        var cachedScore = scoreCache[reference]

        if (cachedScore == null)
        {
            scoreCache[reference] = LeaderboardManagerService
                .getUserRankWithScore(player.uniqueId, reference)
                .join()
            cachedScore = scoreCache[reference]!!
        }

        val personalScore = listOf(
            "${CC.B_PRI}Your score: ${CC.WHITE}${cachedScore.second?.run { Numbers.format(this) }  ?: "${CC.D_GRAY}Loading..."} ${
                CC.GRAY + (cachedScore.first?.run { "[#${Numbers.format(this + 1)}]" } ?: "")
            }"
        )

        return personalScore + LeaderboardManagerService
            .getCachedLeaderboards(reference)
            .mapIndexed { index, entry ->
                "${CC.PRI}#${index + 1}. ${CC.WHITE}${
                    ScalaStoreUuidCache.username(entry.uniqueId)
                } ${CC.GRAY}- ${CC.PRI}${
                    Numbers.format(entry.value)
                }"
            }
    }

    override fun itemDescriptionOf(player: Player, kit: Kit) = localDescriptionOf(
        player,
        Reference(leaderboardType = menuState, kitID = kit.id)
    )

    override fun getGlobalButtons(player: Player): Map<Int, Button>
    {
        val buttons = super.getGlobalButtons(player)
            ?.toMutableMap() ?: mutableMapOf()

        buttons[5] = ItemBuilder
            .copyOf(
                object : TexturedHeadButton(Constants.GLOBE_ICON) {}
                    .getButtonItem(player)
            )
            .name("${CC.PRI}Toggle View")
            .addToLore(
                "${CC.GRAY}Select one of the following",
                "${CC.GRAY}views to see other leaderboards!",
                "",
                "${CC.WHITE}Current view:",
            )
            .apply {
                for (type in ReferenceLeaderboardType.entries)
                {
                    if (menuState == type)
                    {
                        addToLore("${CC.GREEN}► ${type.displayName}")
                    } else
                    {
                        addToLore("${CC.GRAY}${type.displayName}")
                    }
                }
            }
            .addToLore(
                "",
                "${CC.GREEN}Click to scroll through!"
            )
            .toButton { _, type ->
                menuState = if (type!!.isRightClick)
                    menuState.previous() else
                    menuState.next()

                openMenu(player)
            }

        buttons[3] = ItemBuilder
            .of(Material.NETHER_STAR)
            .name("${CC.PRI}Global")
            .setLore(
                localDescriptionOf(player, Reference(menuState, null))
            )
            .toButton()

        return buttons
    }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType) = Unit
    override fun getPrePaginatedTitle(player: Player) = menuState.displayName + " Leaderboards"
}
