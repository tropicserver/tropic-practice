package gg.tropic.practice.kit

import gg.tropic.practice.games.QueueType
import gg.tropic.practice.kit.feature.FeatureFlag
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
data class Kit(
    val id: String,
    var displayName: String,
    var description: String = "",
    var displayIcon: ItemStack = ItemBuilder
        .of(Material.IRON_SWORD)
        .build(),
    var enabled: Boolean = false,
    var armorContents: Array<ItemStack?> = arrayOfNulls(4),
    var contents: Array<ItemStack?> = arrayOfNulls(36),
    var additionalContents: Array<ItemStack?> = arrayOfNulls(27),
    var potionEffects: MutableMap<String, Int> = mutableMapOf(),
    val features: MutableMap<FeatureFlag, MutableMap<String, String>> = mutableMapOf()
)
{
    @Transient
    private var backingAllowedBlockTypeMappings: List<Pair<Material, Int>>? = null

    val allowedBlockTypeMappings: List<Pair<Material, Int>>
        get()
        {
            if (backingAllowedBlockTypeMappings == null)
            {
                backingAllowedBlockTypeMappings = featureConfigNullable(
                    FeatureFlag.BreakSpecificBlockTypes,
                    "types"
                )?.split(",")
                    ?.map { pair ->
                        val components = pair.split(":")
                        Material.valueOf(components[0]) to (components.getOrNull(1)?.toInt() ?: 0)
                    }
            }

            return backingAllowedBlockTypeMappings!!
        }

    @Transient
    private var backingQueueSizes: List<Pair<Int, List<QueueType>>>? = null

    val queueSizes: List<Pair<Int, List<QueueType>>>
        get()
        {
            if (backingQueueSizes == null)
            {
                backingQueueSizes = featureConfig(
                    FeatureFlag.QueueSizes,
                    key = "sizes"
                )
                    .split(",")
                    .map { sizeModel ->
                        val split = sizeModel.split(":")
                        split[0].toInt() to (split.getOrNull(1)
                            ?.split("+")
                            ?.map(QueueType::valueOf)
                            ?: listOf(QueueType.Casual))
                    }
            }

            return backingQueueSizes!!
        }

    fun features(flag: FeatureFlag) = features.containsKey(flag)
    fun featureConfig(flag: FeatureFlag, key: String) =
        features[flag]?.get(key) ?: flag.schema[key]!!

    fun featureConfigNullable(flag: FeatureFlag, key: String) = features[flag]?.get(key)

    fun populate(player: Player)
    {
        player.inventory.clear()
        player.inventory.armorContents = armorContents
        player.inventory.contents = contents

        if (player.activePotionEffects.isNotEmpty())
        {
            player.activePotionEffects
                .forEach { effect ->
                    player.removePotionEffect(effect.type)
                }
        }

        if (potionEffects.isNotEmpty())
        {
            potionEffects.forEach { (t, u) ->
                player.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(t), Int.MAX_VALUE, u
                ))
            }
        }
    }

    fun populateAndUpdate(player: Player)
    {
        populate(player)
        player.updateInventory()
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kit

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (displayIcon != other.displayIcon) return false
        if (enabled != other.enabled) return false
        if (!armorContents.contentEquals(other.armorContents)) return false
        if (!contents.contentEquals(other.contents)) return false
        if (!additionalContents.contentEquals(other.additionalContents)) return false
        if (features != other.features) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + displayIcon.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + armorContents.contentHashCode()
        result = 31 * result + contents.contentHashCode()
        result = 31 * result + additionalContents.contentHashCode()
        result = 31 * result + features.hashCode()
        return result
    }
}
