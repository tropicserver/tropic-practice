package gg.tropic.practice.settings

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.settings.SettingCategory
import gg.scala.basics.plugin.settings.SettingContainer
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.friendship.FriendshipStateSetting
import gg.tropic.practice.settings.particles.FlightEffectSetting
import gg.tropic.practice.settings.restriction.RangeRestriction
import gg.tropic.practice.settings.scoreboard.LobbyScoreboardView
import gg.tropic.practice.settings.scoreboard.ScoreboardStyle
import net.evilblock.cubed.scoreboard.Scoreboard
import net.evilblock.cubed.scoreboard.ScoreboardHandler
import net.evilblock.cubed.scoreboard.ScoreboardListeners
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.DisplaySlot

/**
 * @author GrowlyX
 * @since 10/16/2022
 */
@Service
@IgnoreAutoScan
@SoftDependency("ScBasics")
object DuelsSettingCategory : SettingCategory
{
    const val DUEL_SETTING_PREFIX = "tropicprac"

    override val items = listOf(
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:duel-requests-fr"
            displayName = "Duel requests"

            clazz = FriendshipStateSetting::class.java
            default = FriendshipStateSetting.Enabled

            description += "Allows you to toggle settings"
            description += "for incoming duel requests."

            item = ItemBuilder.of(Material.DIAMOND_SWORD)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:duel-sounds"
            displayName = "Duel sounds"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.ENABLED

            description += "Allows you to be played a"
            description += "notification sound when"
            description += "you receive a duel request."

            item = ItemBuilder.of(Material.NOTE_BLOCK)
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
            id = "$DUEL_SETTING_PREFIX:spawn-visibility"
            displayName = "Player visibility"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Allows you to view or"
            description += "hide players at spawn."

            displayPredicate = {
                it.hasPermission("practice.show-spawn-visibility")
            }

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
            id = "$DUEL_SETTING_PREFIX:restriction-ping"
            displayName = "Ping range"

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
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:flight-effect"
            displayName = "Flight Effects"

            clazz = FlightEffectSetting::class.java
            default = FlightEffectSetting.None

            description += "Have a certain particle"
            description += "floating below your feet"
            description += "as you move!"

            displayPredicate = {
                it.hasPermission("practice.flight-effects")
            }

            item = ItemBuilder.of(Material.SUGAR)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:silent-spectator"
            displayName = "Silent Spectator"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.DISABLED

            description += "Prevents other players"
            description += "from viewing you as a spectator."

            displayPredicate = {
                it.hasPermission("practice.silent-spectator")
            }

            item = ItemBuilder.of(XMaterial.POTION)
        },
        SettingContainer.buildEntry {
            id = "$DUEL_SETTING_PREFIX:scoreboard-style"
            displayName = "Scoreboard Style"

            clazz = ScoreboardStyle::class.java
            default = ScoreboardStyle.Default

            description += "Allows you to"
            description += "change your scoreboard style."

            displayPredicate = {
                it.hasPermission("practice.style.donator")
            }

            postChange = {
                if (layout(it) == ScoreboardStyle.Disabled)
                {
                    it.scoreboard = Bukkit
                        .getScoreboardManager()
                        .newScoreboard
                } else
                {
                    if (it.scoreboard.getObjective(DisplaySlot.SIDEBAR) == null)
                    {
                        ScoreboardListeners.onPlayerJoinEvent(
                            event = PlayerJoinEvent(it, "")
                        )
                    }
                }
            }

            item = ItemBuilder.of(Material.BREWING_STAND_ITEM)
        },
    )

    override fun display(player: Player) = true

    override val displayName = "Practice"
    override val description = listOf(
        "Practice style, privacy, matchmaking,",
        "perks, and other options."
    )
}
