package gg.tropic.practice.category

import gg.scala.basics.plugin.settings.SettingCategory
import gg.scala.basics.plugin.settings.SettingContainer
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.category.restriction.RangeRestriction
import gg.tropic.practice.category.scoreboard.LobbyScoreboardView
import gg.tropic.practice.category.visibility.SpawnPlayerVisibility
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
@Service
@IgnoreAutoScan
@SoftDependency("ScBasics")
object DuelsSettingCategory : SettingCategory
{
    private const val DUEL_SETTING_PREFIX = "duels"

    override val items = listOf(
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:duel-requests"
            displayName = "Duel requests"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.ENABLED

            description += "Allows you to disable"
            description += "incoming duel requests."

            item = ItemBuilder.of(Material.DIODE)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:lobby-scoreboard-view"
            displayName = "Lobby scoreboard view"

            clazz = LobbyScoreboardView::class.java
            default = LobbyScoreboardView.None

            description += "Select an extra-info"
            description += "category for your lobby"
            description += "scoreboard"

            displayPredicate = {
                it.hasPermission("practice.lobby.scoreboard.views")
            }

            item = ItemBuilder.of(Material.BOOK)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:ranked-restriction-ping"
            displayName = "Ranked Queue ${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Ping range"

            clazz = RangeRestriction::class.java
            default = RangeRestriction.None

            description += "Select the the maximum"
            description += "value your opponent's"
            description += "ping can differ by."
            description += ""
            description += "${CC.WHITE}Range: ${CC.GREEN}ping ± value"

            displayPredicate = {
                it.hasPermission("practice.ranked.restriction.ping")
            }

            item = ItemBuilder.of(Material.EXP_BOTTLE)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:spawn-flight"
            displayName = "Spawn flight"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Allows you to"
            description += "fly at spawn."

            item = ItemBuilder.of(Material.FEATHER)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:player-visibility"
            displayName = "Player visibility"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Allows you to see"
            description += "other players at spawn."

            postChange = {
                VisibilityHandler.update(it)
            }

            item = ItemBuilder.of(Material.EYE_OF_ENDER)
        }
    )

    override fun display(player: Player) = true

    override val displayName = "Practice"
    override val description = listOf(
        "Practice settings."
    )
}
