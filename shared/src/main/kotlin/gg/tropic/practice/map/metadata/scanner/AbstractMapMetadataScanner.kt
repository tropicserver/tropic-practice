package gg.tropic.practice.map.metadata.scanner

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
abstract class AbstractMapMetadataScanner<T : AbstractMapMetadata>
{
    abstract val type: String

    abstract fun scan(id: String, models: List<MapSignMetadataModel>): T
}
