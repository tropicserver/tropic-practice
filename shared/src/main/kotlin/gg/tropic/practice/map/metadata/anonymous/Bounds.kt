package gg.tropic.practice.map.metadata.anonymous

import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.BlockState

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
data class Bounds(
    val lowerLeft: Position,
    val upperRight: Position
)
{
    fun getTileEntities(world: World): List<BlockState>
    {
        return getChunks(world)
            .flatMap {
                it.tileEntities.toList()
            }
    }

    fun getChunks(world: World): List<Chunk>
    {
        val chunks = mutableListOf<Chunk>()
        val minX = lowerLeft.x.toInt()
        val maxX = upperRight.x.toInt()
        val minZ = lowerLeft.z.toInt()
        val maxZ = upperRight.z.toInt()

        for (x in minX..maxX)
        {
            for (z in minZ..maxZ)
            {
                if (!world.isChunkLoaded(x, z))
                    world.loadChunk(x, z, false)

                chunks.add(
                    world.getChunkAt(x, z)
                )
            }
        }

        return chunks
    }

    operator fun contains(position: Position): Boolean
    {
        return position.x >= lowerLeft.x &&
            position.x <= upperRight.x &&
            position.z >= lowerLeft.z &&
            position.z <= upperRight.z
    }
}
