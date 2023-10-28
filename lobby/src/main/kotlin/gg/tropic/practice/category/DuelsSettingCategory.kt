package gg.tropic.practice.category

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.SettingCategory
import gg.scala.basics.plugin.settings.SettingContainer
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.category.restriction.RangeRestriction
import gg.tropic.practice.category.scoreboard.LobbyScoreboardView
import gg.tropic.practice.settings.ChatVisibility
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

            item = ItemBuilder.of(Material.DIAMOND_SWORD)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:allow-spectators"
            displayName = "Allow spectators"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.ENABLED

            description += "Allows you to prevent"
            description += "players from spectating"
            description += "any of your matches."

            item = ItemBuilder.of(Material.GLASS_BOTTLE)
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
            id = "$DUEL_SETTING_PREFIX:spawn-flight"
            displayName = "Spawn flight"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Allows you to fly"
            description += "around spawn."

            postChange = {
                val flightEnabled = BasicsProfileService.find(it)
                    ?.setting(
                        "duels:spawn-flight",
                        StateSettingValue.DISABLED
                    )

                val flightSetting =
                    flightEnabled == StateSettingValue.ENABLED

                it.allowFlight = flightSetting
                it.isFlying = flightSetting
            }

            item = ItemBuilder.of(Material.FEATHER)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:spawn-visibility"
            displayName = "Player visibility"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Allows you to view or"
            description += "hide players at spawn."

            postChange = {
                VisibilityHandler.update(it)
            }

            item = ItemBuilder.of(Material.EYE_OF_ENDER)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:chat-visibility"
            displayName = "Chat visibility"

            clazz = ChatVisibility::class.java
            default = ChatVisibility.Global

            description += "Filter chat messages you"
            description += "see in game."

            item = ItemBuilder.of(Material.PAPER)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:ranked-restriction-ping"
            displayName = "Ranked Queue ${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Ping range"

            clazz = RangeRestriction::class.java
            default = RangeRestriction.None

            description += "Select the maximum value"
            description += "your opponent's ping can"
            description += "differ by."
            description += ""
            description += "${CC.WHITE}Range: ${CC.GREEN}ping ± value"

            displayPredicate = {
                it.hasPermission("practice.ranked.restriction.ping")
            }

            item = ItemBuilder.of(Material.EXP_BOTTLE)
        }
    )

    override fun display(player: Player) = true

    override val displayName = "Practice"
    override val description = listOf(
        "Practice settings."
    )
}
