package gg.tropic.practice.ladder

import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
enum class DuelLadder(
    val flags: List<DuelLadderFlag> = listOf()
) : DuelLadderInventoryPopulator
{
    UHC(
        flags = listOf(
            DuelLadderFlag.HeartsBelowNametag,
            DuelLadderFlag.PlaceBlocks,
        )
    )
    {
        override fun populate(player: Player)
        {
            val inventory = player.inventory

            val heads = ItemBuilder
                .copyOf(this.goldenHead)
                .amount(3)
                .build()

            inventory.addItem(ItemBuilder(Material.DIAMOND_SWORD).enchant(Enchantment.DAMAGE_ALL, 3).build())
            inventory.addItem(ItemStack(Material.FISHING_ROD))
            inventory.addItem(ItemStack(Material.LAVA_BUCKET))
            inventory.addItem(ItemBuilder(Material.BOW).enchant(Enchantment.ARROW_DAMAGE, 2).build())
            inventory.addItem(ItemStack(Material.WATER_BUCKET))
            inventory.addItem(heads)
            inventory.addItem(ItemStack(Material.GOLDEN_APPLE, 6))
            inventory.addItem(ItemStack(Material.WOOD, 64))
            inventory.addItem(ItemStack(Material.ARROW, 16))

            inventory.setItem(9, ItemStack(Material.DIAMOND_AXE))
            inventory.setItem(29, ItemStack(Material.LAVA_BUCKET))
            inventory.setItem(31, ItemStack(Material.WATER_BUCKET))
            inventory.setItem(34, ItemStack(Material.WOOD, 64))

            inventory.helmet =
                ItemBuilder(Material.DIAMOND_HELMET).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2)
                    .enchant(
                        Enchantment.DURABILITY, 3
                    ).build()
            inventory.chestplate =
                ItemBuilder(Material.DIAMOND_CHESTPLATE).enchant(Enchantment.PROTECTION_PROJECTILE, 2)
                        .enchant(
                        Enchantment.DURABILITY, 3
                    ).build()
            inventory.leggings =
                ItemBuilder(Material.DIAMOND_LEGGINGS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2)
                    .enchant(
                        Enchantment.DURABILITY, 3
                    ).build()
            inventory.boots =
                ItemBuilder(Material.DIAMOND_BOOTS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2).enchant(
                    Enchantment.DURABILITY, 3
                ).build()
        }
    },

    Sumo(
        flags = listOf(
            DuelLadderFlag.DoNotTakeHealth,
            DuelLadderFlag.FrozenOnStart
        )
    )
    {
        override fun populate(player: Player)
        {

        }
    },

    Bow(
        flags = listOf(
            DuelLadderFlag.DoNotRemoveArmor
        )
    )
    {
        override fun populate(player: Player)
        {
            val inventory = player.inventory
            inventory.helmet = ItemStack(Material.LEATHER_HELMET)
            inventory.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
            inventory.leggings = ItemStack(Material.LEATHER_LEGGINGS)
            inventory.boots = ItemStack(Material.LEATHER_BOOTS)

            inventory.addItem(
                ItemBuilder(Material.BOW)
                    .enchant(Enchantment.ARROW_DAMAGE, 1)
                    .enchant(Enchantment.ARROW_INFINITE, 1)
                    .enchant(Enchantment.DURABILITY, 3)
                    .build()
            )

            inventory.addItem(ItemStack(Material.ENDER_PEARL))
            inventory.setItem(8, ItemStack(Material.ARROW))
        }
    };

    val goldenHead by lazy {
        val playerHead = ItemStack(
            Material.SKULL_ITEM, 1, 3.toShort()
        )

        val meta = playerHead.itemMeta
        meta.displayName = ChatColor.GOLD.toString() + "Golden Head"
        playerHead.itemMeta = meta

        val metaData = playerHead.itemMeta as SkullMeta
        metaData.owner = "GoldenCutler"

        playerHead.itemMeta = metaData
        return@lazy playerHead
    }
}
