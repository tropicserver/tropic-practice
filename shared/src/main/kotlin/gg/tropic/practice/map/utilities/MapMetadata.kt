package gg.tropic.practice.map.utilities

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
data class MapMetadata(
    val metadataSignLocations: List<Vector>,
    val metadata: List<AbstractMapMetadata>
)
{
    fun clearSignLocations(world: World): CompletableFuture<Void>
    {
        val future = CompletableFuture<Void>()
        val blocks = metadataSignLocations
            .mapNotNull {
                world.getBlockAt(it.blockX, it.blockY, it.blockZ)
            }

        Tasks.sync {
            blocks.forEach {
                it.type = Material.AIR
            }

            future.complete(null)
        }

        return future
    }
}
