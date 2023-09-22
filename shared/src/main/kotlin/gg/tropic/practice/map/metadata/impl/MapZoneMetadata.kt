package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapZoneMetadata(
    override val id: String,
    var lower: Location,
    var top: Location,
    var bounds: Cuboid = Cuboid(lower, top)
) : AbstractMapMetadata()
{
    override fun getAbstractType() = MapZoneMetadata::class.java

    override fun adjustLocations(xDiff: Double, zDiff: Double): AbstractMapMetadata
    {
        return MapZoneMetadata(
            id = id,
            lower = this.lower.clone().add(xDiff, 0.0, zDiff),
            top = this.top.clone().add(xDiff, 0.0, zDiff)
        )
    }
}
