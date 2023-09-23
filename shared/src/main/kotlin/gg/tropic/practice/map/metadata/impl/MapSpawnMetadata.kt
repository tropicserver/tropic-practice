package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.anonymous.Position

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapSpawnMetadata(
    override val id: String,
    var position: Position
) : AbstractMapMetadata()
{
    override fun getAbstractType() = MapSpawnMetadata::class.java
}
