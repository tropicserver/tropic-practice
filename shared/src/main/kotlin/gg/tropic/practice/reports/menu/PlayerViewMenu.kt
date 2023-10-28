package gg.tropic.practice.reports.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.games.GameReport
import gg.tropic.practice.games.GameReportSnapshot
import gg.tropic.practice.reports.menu.utility.RomanNumerals
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.buttons.TexturedHeadButton
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 9/16/2022
 */
class PlayerViewMenu(
    private val gameReport: GameReport,
    private val reportOf: UUID,
    private val snapshot: GameReportSnapshot,
    private val originalMenu: Menu? = null
) : Menu("Viewing player ${reportOf.username()}...")
{
    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            if (originalMenu == null)
            {
                return
            }

            Tasks.sync {
                SelectPlayerMenu(gameReport, originalMenu).openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        for (i in 45 until 54)
        {
            buttons[i] = PaginatedMenu.PLACEHOLDER
        }

        buttons[47] = ItemBuilder.of(XMaterial.GLISTERING_MELON_SLICE)
            .name("${CC.SEC}Health: ${CC.GREEN}${"%.1f".format(snapshot.health.toFloat())} ${Constants.HEART_SYMBOL}")
            .toButton()

        buttons[48] = ItemBuilder.of(Material.COOKED_BEEF)
            .name("${CC.SEC}Food Level: ${CC.GREEN}${"%.1f".format(snapshot.foodLevel.toFloat())}")
            .toButton()

        buttons[49] = if (snapshot.healthPotions > 0)
        {
            ItemBuilder.of(XMaterial.POTION)
                .data(16421)
                .name("${CC.SEC}Health Potions: ${CC.GREEN}${snapshot.healthPotions}")
                .toButton()
        } else if (snapshot.mushroomStews > 0)
        {
            ItemBuilder.of(XMaterial.MUSHROOM_STEW)
                .name("${CC.SEC}Stews: ${CC.GREEN}${snapshot.mushroomStews}")
                .toButton()
        } else
        {
            PaginatedMenu.PLACEHOLDER
        }

        buttons[50] = ItemBuilder.of(XMaterial.BREWING_STAND)
            .name("${CC.B_PRI}Potion Effects")
            .apply {
                if (snapshot.potionEffects.isEmpty())
                {
                    addToLore("${CC.RED}No potion effects!")
                    return@apply
                }

                for (potionEffect in snapshot.potionEffects)
                {
                    addToLore("${CC.WHITE}${
                        potionEffect.type.name
                            .replace("_", " ")
                            .split(" ")
                            .joinToString(" ") {
                                it.lowercase().capitalize()
                            }
                    }${
                        if (potionEffect.amplifier > 0) " ${RomanNumerals.toRoman(potionEffect.amplifier)}" else ""
                    } ${CC.GRAY}(${
                        if (potionEffect.duration > 10000) "**:**" else TimeUtil.formatIntoMMSS(potionEffect.duration)
                    })")
                }
            }
            .toButton()

        buttons[51] = ItemBuilder
            .of(Material.PAPER)
            .name(
                "${CC.B_PRI}Extra Information"
            )
            .addToLore(
                "${CC.WHITE}Duration: ${CC.PRI}${
                    TimeUtil.formatIntoMMSS((gameReport.duration / 1000).toInt())
                }"
            )
            .apply {
                val extraInformation = gameReport.extraInformation[reportOf]
                    ?: return@apply

                for (information in extraInformation)
                {
                    if (information.value.size == 1)
                    {
                        addToLore("${CC.WHITE}${information.key}: ${CC.PRI}${information.value.values.first()}")
                        continue
                    }

                    addToLore("", "${CC.PRI}${information.key}")
                    for (entry in information.value)
                    {
                        addToLore(" ${CC.WHITE}${entry.key}: ${CC.PRI}${entry.value}")
                    }
                }
            }
            .toButton()

        val indexes = gameReport.winners + gameReport.losers
        val index = indexes.indexOf(reportOf)

        if (index + 1 < indexes.size)
        {
            val nextPlayer = indexes[index + 1]
            val snapshot = gameReport.snapshots[nextPlayer]!!

            buttons[53] = ItemBuilder
                .copyOf(
                    object : TexturedHeadButton(Constants.WOOD_ARROW_RIGHT_TEXTURE){}.getButtonItem(player)
                )
                .name(
                    "${CC.B_SEC}${nextPlayer.username()}'s Inventory"
                )
                .addToLore(
                    "",
                    "${CC.SEC}Click to switch inventories!"
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    PlayerViewMenu(gameReport, nextPlayer, snapshot, originalMenu).openMenu(player)
                }
        }

        if (index > 0)
        {
            val nextPlayer = indexes[index - 1]
            val snapshot = gameReport.snapshots[nextPlayer]!!

            buttons[45] = ItemBuilder
                .copyOf(
                    object : TexturedHeadButton(Constants.WOOD_ARROW_LEFT_TEXTURE){}.getButtonItem(player)
                )
                .name(
                    "${CC.B_SEC}${nextPlayer.username()}'s Inventory"
                )
                .addToLore(
                    "",
                    "${CC.SEC}Click to switch inventories!"
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    PlayerViewMenu(gameReport, nextPlayer, snapshot, originalMenu).openMenu(player)
                }
        }

        snapshot.inventoryContents.withIndex()
            .forEach {
                if (it.value.first != null)
                {
                    buttons[it.index] = ItemBuilder
                        .copyOf(it.value.first)
                        .amount(it.value.second)
                        .toButton()
                }
            }

        for (i in 36..39)
        {
            val armor = snapshot.armorContents.getOrNull(i - 36)

            if (armor == null)
            {
                buttons[i] = ItemBuilder
                    .of(Material.BARRIER)
                    .name("${CC.BD_RED}No item")
                    .addToLore(
                        "${CC.WHITE}This player did not have",
                        "${CC.WHITE}an item in this armor slot!"
                    )
                    .toButton()
                continue
            }

            buttons[i] = ItemBuilder
                .copyOf(armor)
                .glow()
                .toButton()
        }

        return buttons
    }

    override fun size(buttons: Map<Int, Button>) = 54
}
