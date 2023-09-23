package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.anonymous.Position
import org.bukkit.block.BlockFace

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapBedMetadata(
    override val id: String,
    var position: Position,
    val face: BlockFace
) : AbstractMapMetadata()
{
    override fun getAbstractType() = MapBedMetadata::class.java
}
