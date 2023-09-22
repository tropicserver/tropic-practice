package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import org.bukkit.Location
import org.bukkit.block.BlockFace

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapBedMetadata(
    override val id: String,
    var location: Location,
    val face: BlockFace
) : AbstractMapMetadata()
{
    override fun getAbstractType() = MapBedMetadata::class.java

    override fun adjustLocations(xDiff: Double, zDiff: Double): AbstractMapMetadata
    {
        return MapBedMetadata(
            id = id, location = this.location.clone().add(xDiff, 0.0, zDiff), face
        )
    }
}
