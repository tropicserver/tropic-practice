package gg.tropic.practice.menu

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.tropic.practice.queue.QueueType
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
    init
    {
        async = true
        autoUpdate = true
    }

    override fun getAutoUpdateTicks() = 500L
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
                if (queueType == QueueType.Ranked) "ELO" else "Daily Streak"
            ).toTypedArray(),
            "",
            "${CC.GREEN}Click to queue!"
        )
    }

    override fun asyncLoadResources(player: Player, callback: (Boolean) -> Unit)
    {
        callback(true)
    }

    private val scoreCache = mutableMapOf<Reference, Pair<Long?, Long?>>()
    private fun local3ScoreDescriptionOf(player: Player, reference: Reference, label: String): List<String>
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
            "${CC.B_PRI}Your $label: ${CC.WHITE}${cachedScore.second?.run { Numbers.format(this) } ?: "${CC.D_GRAY}..."} ${
                CC.GRAY + (cachedScore.first?.run { "[#${Numbers.format(this + 1)}]" } ?: "")
            }"
        )

        return personalScore + LeaderboardManagerService
            .getCachedFormattedLeaderboards(reference)
            .take(3)
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
