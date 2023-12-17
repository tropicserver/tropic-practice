package gg.tropic.practice.menu

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.leaderboards.Reference
import gg.tropic.practice.leaderboards.ReferenceLeaderboardType
import gg.tropic.practice.menu.template.TemplateKitMenu
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.services.GameManagerService
import gg.tropic.practice.services.LeaderboardManagerService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.text.TextSplitter
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
class JoinQueueMenu(
    private val queueType: QueueType,
    private val teamSize: Int
) : TemplateKitMenu()
{
    init
    {
        autoUpdate = true
        autoUpdateInterval = 100L
    }

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
            "${CC.WHITE}Queuing: ${CC.PRI}${metadata.inQueue}",
            "",
            *local3ScoreDescriptionOf(
                player,
                Reference(if (queueType == QueueType.Ranked) ReferenceLeaderboardType.ELO else ReferenceLeaderboardType.CasualWinStreak, kit.id),
                if (queueType == QueueType.Ranked) "ELO" else "Daily Wins"
            ).toTypedArray(),
            "",
            "${CC.GREEN}Click to queue!"
        )
    }

    private val loadFutures = mutableMapOf<Reference, CompletableFuture<Pair<Long?, Long?>>>()
    private val scoreCache = mutableMapOf<Reference, Pair<Long?, Long?>>()

    private fun local3ScoreDescriptionOf(player: Player, reference: Reference, label: String): List<String>
    {
        val cachedScore = scoreCache[reference]

        if (cachedScore == null)
        {
            if (!loadFutures.containsKey(reference))
            {
                loadFutures[reference] = LeaderboardManagerService
                    .getUserRankWithScore(player.uniqueId, reference)
                    .thenApplyAsync {
                        scoreCache[reference] = it
                        it
                    }
            }
        }

        val personalScore = listOf(
            "${CC.B_PRI}Your $label: ${CC.WHITE}${cachedScore?.second?.run { Numbers.format(this) } ?: "${CC.D_GRAY}Loading..."} ${
                CC.GRAY + (cachedScore?.first?.run { "[#${Numbers.format(this + 1)}]" } ?: "")
            }"
        )

        return personalScore + LeaderboardManagerService
            .getCachedLeaderboards(reference)
            .take(3)
            .mapIndexed { index, entry ->
                "${CC.PRI}#${index + 1}. ${CC.WHITE}${
                    ScalaStoreUuidCache.username(entry.uniqueId)
                } ${CC.GRAY}- ${CC.PRI}${
                    Numbers.format(entry.value)
                }"
            }
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
