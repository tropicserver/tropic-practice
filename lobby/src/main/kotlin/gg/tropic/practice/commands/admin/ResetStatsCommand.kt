package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.commands.offlineProfile
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.statistics.GlobalStatistics
import gg.tropic.practice.statistics.KitStatistics
import gg.tropic.practice.statistics.ranked.RankedKitStatistics
import net.evilblock.cubed.util.CC
import org.bukkit.command.CommandSender

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
@AutoRegister
object ResetStatsCommand : ScalaCommand()
{
    enum class StatReset(
        val resetFor: (PracticeProfile, Kit?) -> Unit
    )
    {
        ELO({ profile, kit ->
            profile.rankedStatistics.forEach { (kitID, rankedStats) ->
                if (kit == null || kit.id == kitID)
                {
                    rankedStats.updateELO(1000)
                }
            }
        }),
        CasualStats({ profile, kit ->
            profile.casualStatistics.toMap().forEach { (kitID, _) ->
                if (kit == null || kit.id == kitID)
                {
                    profile.casualStatistics[kitID] = KitStatistics()
                }
            }
        }),
        RankedStats({ profile, kit ->
            profile.rankedStatistics.toMap().forEach { (kitID, _) ->
                if (kit == null || kit.id == kitID)
                {
                    profile.rankedStatistics[kitID] = RankedKitStatistics()
                }
            }
        }),
        GlobalStats({ profile, _ ->
            profile.globalStatistics = GlobalStatistics()
        }),
        AllStats({ profile, kit ->
            RankedStats.resetFor(profile, kit)
            GlobalStats.resetFor(profile, kit)
            CasualStats.resetFor(profile, kit)
        })
    }

    @CommandAlias("resetstats")
    @CommandCompletion("@mip-players * @kits")
    @CommandPermission("practice.command.resetstats")
    fun onResetStats(
        player: CommandSender,
        target: AsyncLemonPlayer,
        spec: StatReset,
        @Optional kit: Kit?
    ) = target.validatePlayers(player, false) {
        val profile = it.uniqueId.offlineProfile
        spec.resetFor(profile, kit)

        profile.save().join()

        player.sendMessage(
            "${CC.GREEN}You have reset the ${CC.BOLD}${
                spec.name
            }${CC.GREEN} for ${CC.WHITE}${it.name}${CC.GREEN}${
                if (kit != null) " on kit ${CC.PRI}${kit.id}${CC.GREEN}" else ""
            }."
        )
    }
}
