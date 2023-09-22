package gg.tropic.practice.map

import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Defines a map. Maps and available replications are decoupled, so
 * we don't have to worry about constantly synchronizing the [MapService]
 * when new replications are available or if a replication is deleted.
 *
 * @author GrowlyX
 * @since 9/21/2023
 */
data class Map(
    override val identifier: UUID,
    val name: String,
    val bounds: Cuboid,
    val metadata: List<AbstractMapMetadata>,
    var displayName: String,
    val displayIcon: ItemStack = ItemBuilder
        .of(Material.MAP)
        .build(),
    val associatedSlimeTemplate: String,
    val associatedKitGroups: MutableSet<String> =
        mutableSetOf("__default__")
) : IDataStoreObject
