package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapSpawnMetadata(
    override val id: String,
    var location: Location
) : AbstractMapMetadata()
{
    override fun getAbstractType() = MapSpawnMetadata::class.java

    override fun adjustLocations(xDiff: Double, zDiff: Double): AbstractMapMetadata
    {
        return MapSpawnMetadata(
            id = id, location = this.location.clone().add(xDiff, 0.0, zDiff)
        )
    }
}
