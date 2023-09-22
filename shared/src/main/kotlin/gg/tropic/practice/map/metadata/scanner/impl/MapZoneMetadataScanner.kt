package gg.tropic.practice.map.metadata.scanner.impl

import gg.tropic.practice.map.metadata.impl.MapZoneMetadata
import gg.tropic.practice.map.metadata.scanner.AbstractMapMetadataScanner
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
object MapZoneMetadataScanner : AbstractMapMetadataScanner<MapZoneMetadata>()
{
    override val type = "zone"

    override fun scan(
        id: String,
        models: List<MapSignMetadataModel>
    ): MapZoneMetadata
    {
        return MapZoneMetadata(
            id, models[0].location, models[1].location
        )
    }
}
