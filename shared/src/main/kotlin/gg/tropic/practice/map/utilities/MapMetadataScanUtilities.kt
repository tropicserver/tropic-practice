package gg.tropic.practice.map.utilities

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.sign.parseIntoMetadata
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Sign

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
object MapMetadataScanUtilities
{
    fun populateMapMetadata(world: World)
    {
        val scheduledRemoval = mutableListOf<Block>()
        // TODO: loadedChunks might not contain the map chunks
        val blocks = world.loadedChunks
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
                scheduledRemoval += it.location.block
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

        // TODO: we need to run this every time a
        //  template replication is created lol
        Tasks.sync {
            for (sign in scheduledRemoval)
            {
                sign.type = Material.AIR
            }
        }
    }
}
