package gg.tropic.practice.map

import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.map.metadata.anonymous.Bounds
import gg.tropic.practice.map.metadata.anonymous.toPosition
import gg.tropic.practice.map.metadata.impl.MapLevelMetadata
import gg.tropic.practice.map.metadata.impl.MapSpawnMetadata
import gg.tropic.practice.map.metadata.impl.MapZoneMetadata
import gg.tropic.practice.map.utilities.MapMetadata
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

/**
 * Defines a map. Maps and available replications are decoupled, so
 * we don't have to worry about constantly synchronizing the [MapService]
 * when new replications are available or if a replication is deleted.
 *
 * @author GrowlyX
 * @since 9/21/2023
 */
data class Map(
    val name: String,
    val bounds: Bounds,
    val metadata: MapMetadata,
    var displayName: String,
    val displayIcon: ItemStack = ItemBuilder
        .of(Material.MAP)
        .build(),
    val associatedSlimeTemplate: String,
    val associatedKitGroups: MutableSet<String> =
        mutableSetOf("__default__")
)
{
    fun findMapLevelRestrictions() = metadata
        .metadata
        .filterIsInstance<MapLevelMetadata>()
        .firstOrNull()

    fun findZoneContainingEntity(entity: Entity) = metadata
        .metadata
        .filterIsInstance<MapZoneMetadata>()
        .firstOrNull {
            it.bounds.contains(entity.location.toPosition())
        }

    fun findSpawnLocationMatchingTeam(team: GameTeamSide) = metadata
        .metadata
        .filterIsInstance<MapSpawnMetadata>()
        .firstOrNull {
            it.id == team.name.lowercase()
        }
        ?.position
}
