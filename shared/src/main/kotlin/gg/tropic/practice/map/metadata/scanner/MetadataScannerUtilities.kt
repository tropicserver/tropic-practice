package gg.tropic.practice.map.metadata.scanner

import gg.tropic.practice.map.metadata.scanner.impl.MapLevelMetadataScanner
import gg.tropic.practice.map.metadata.scanner.impl.MapSpawnMetadataScanner
import gg.tropic.practice.map.metadata.scanner.impl.MapZoneMetadataScanner
import gg.tropic.practice.map.metadata.scanner.impl.MapBedMetadataScanner

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
object MetadataScannerUtilities
{
    private val scanners = mutableListOf<AbstractMapMetadataScanner<*>>(
        MapZoneMetadataScanner,
        MapSpawnMetadataScanner,
        MapLevelMetadataScanner,
        MapBedMetadataScanner
    )

    fun matches(type: String) =
        scanners.firstOrNull {
            it.type.equals(type, true)
        }
}
