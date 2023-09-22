package gg.tropic.practice.map.utilities

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.sign.parseIntoMetadata
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import org.bukkit.block.Sign
import org.bukkit.util.Vector

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
object MapMetadataScanUtilities
{
    fun buildMetadataFor(bounds: Cuboid): MapMetadata
    {
        val scheduledRemoval = mutableListOf<Vector>()
        val blocks = bounds.getChunks()
            .flatMap {
                it.tileEntities.toList()
            }

        val modelMappings = blocks
            .filterIsInstance<Sign>()
            .mapNotNull {
                val metadata = it.lines.toList()
                    .parseIntoMetadata(it.location)

                metadata
            }
            .onEach {
                scheduledRemoval += it.location.block.location.toVector()
            }
            .groupBy { it.id }

        val metadata = mutableListOf<AbstractMapMetadata>()
        for (modelMapping in modelMappings)
        {
            val scanner = modelMapping
                .value.first().scanner

            metadata += scanner.scan(
                modelMapping.key, modelMapping.value
            )
        }

        return MapMetadata(
            metadataSignLocations = scheduledRemoval,
            metadata = metadata
        )
    }
}
