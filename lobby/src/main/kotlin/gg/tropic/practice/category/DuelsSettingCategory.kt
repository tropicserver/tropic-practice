package gg.tropic.practice.category

import gg.scala.basics.plugin.settings.SettingCategory
import gg.scala.basics.plugin.settings.SettingContainer
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.tropic.practice.category.scoreboard.LobbyScoreboardView
import net.evilblock.cubed.util.bukkit.ItemBuilder
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
    override val items = listOf(
        SettingContainer.buildEntry {
            id = "duel-requests"
            displayName = "Duel requests"

            clazz = StateSettingValue::class.java
            default = StateSettingValue.ENABLED

            description += "Allows you to disable"
            description += "incoming duel requests."

            item = ItemBuilder.of(Material.DIODE)
        },
        SettingContainer.buildEntry {
            id = "lobby-scoreboard-view"
            displayName = "Lobby Scoreboard View"

            clazz = LobbyScoreboardView::class.java
            default = LobbyScoreboardView.None

            description += "Select an extra-info"
            description += "category for your lobby"
            description += "scoreboard"

            displayPredicate = {
                it.hasPermission("practice.lobby.scoreboard.views")
            }

            item = ItemBuilder.of(Material.BOOK)
        }
    )

    override fun display(player: Player) = true

    override val displayName = "Practice"
    override val description = listOf(
        "Practice settings."
    )
}
