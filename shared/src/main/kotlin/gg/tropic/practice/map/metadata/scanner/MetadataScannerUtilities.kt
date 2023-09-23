package gg.tropic.practice.map.metadata.scanner

import gg.tropic.practice.map.metadata.scanner.impl.MapLevelMetadataScanner
import gg.tropic.practice.map.metadata.scanner.impl.MapSpawnMetadataScanner
import gg.tropic.practice.map.metadata.scanner.impl.MapZoneMetadataScanner

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
object MetadataScannerUtilities
{
    private val scanners = mutableListOf<AbstractMapMetadataScanner<*>>(
        // TODO: dynamic scan with @Service please
        MapZoneMetadataScanner,
        MapSpawnMetadataScanner,
        MapLevelMetadataScanner
    )

    fun matches(type: String) =
        scanners.firstOrNull {
            it.type.equals(type, true)
        }
}
