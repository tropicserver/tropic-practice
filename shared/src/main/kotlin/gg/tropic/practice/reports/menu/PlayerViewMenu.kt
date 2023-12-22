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
import net.evilblock.cubed.util.nms.MinecraftProtocol
import net.evilblock.cubed.util.nms.MinecraftReflection
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
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

        val viewerVersionIs17 = MinecraftProtocol.getPlayerVersion(player) == 5

        buttons[47] = ItemBuilder.of(XMaterial.GLISTERING_MELON_SLICE)
            .name("${CC.B_PRI}Health: ${CC.RED}${"%.1f".format(snapshot.health.toFloat())}${Constants.HEART_SYMBOL}")
            .toButton()

        buttons[48] = ItemBuilder.of(Material.COOKED_BEEF)
            .name("${CC.B_PRI}Food Level: ${CC.GOLD}${"%.1f".format(snapshot.foodLevel.toFloat())}")
            .toButton()

        buttons[49] = if (snapshot.healthPotions > 0)
        {
            ItemBuilder.of(XMaterial.POTION)
                .data(16421)
                .name("${CC.B_PRI}Health Potions: ${CC.SEC}${snapshot.healthPotions}")
                .addToLore(
                    "${CC.SEC}Total Used: ${CC.PRI}${snapshot.totalPotionsUsed}",
                    "",
                    "${CC.PRI}${CC.UNDERLINE}Stats:",
                    "${CC.SEC}Hit: ${CC.PRI}${snapshot.hitPotions}",
                    "${CC.SEC}Missed: ${CC.PRI}${snapshot.missedPotions}",
                    "${CC.SEC}Accuracy: ${CC.PRI}${
                        "%.2f".format(
                            if (snapshot.totalPotionsUsed == 0) 0.0 else ((snapshot.hitPotions / snapshot.totalPotionsUsed.toDouble()) * 100.0)
                        ) 
                    }%",
                    "",
                    "${CC.SEC}Wasted Heal: ${CC.RED}${
                        "%.2f".format(snapshot.wastedHeals.toFloat())
                    }${Constants.HEART_SYMBOL}"
                )
                .addFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES)
                .toButton()
        } else if (snapshot.mushroomStews > 0)
        {
            ItemBuilder.of(XMaterial.MUSHROOM_STEW)
                .name("${CC.B_SEC}Stews: ${CC.PRI}${snapshot.mushroomStews}")
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
                                it.lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }
                    }${
                        if (potionEffect.amplifier > 0) " ${RomanNumerals.toRoman(potionEffect.amplifier)}" else ""
                    } ${CC.GRAY}(${
                        if (potionEffect.duration > 5000) "**:**" else TimeUtil.formatIntoMMSS(potionEffect.duration)
                    })")
                }
            }
            .toButton()

        buttons[51] = ItemBuilder
            .of(Material.PAPER)
            .name(
                "${CC.B_PRI}Game Statistics"
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

                    addToLore("", "${CC.PRI}${CC.UNDERLINE}${information.key}:")
                    for (entry in information.value)
                    {
                        addToLore("${CC.WHITE}${entry.key}: ${CC.PRI}${entry.value}")
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
                .let {
                    if (viewerVersionIs17)
                    {
                        return@let it.of(Material.PAPER)
                    }

                    return@let it.copyOf(
                        object : TexturedHeadButton(Constants.WOOD_ARROW_RIGHT_TEXTURE)
                        {}.getButtonItem(player)
                    )
                }
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
                .let {
                    if (viewerVersionIs17)
                    {
                        return@let it.of(Material.PAPER)
                    }

                    return@let it.copyOf(
                        object : TexturedHeadButton(Constants.WOOD_ARROW_LEFT_TEXTURE)
                        {}.getButtonItem(player)
                    )
                }
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
                if (viewerVersionIs17)
                {
                    continue
                }

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
