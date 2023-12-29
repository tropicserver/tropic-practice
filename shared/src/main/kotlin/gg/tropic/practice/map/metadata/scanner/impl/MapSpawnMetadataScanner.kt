package gg.tropic.practice.map.metadata.scanner.impl

import gg.tropic.practice.map.metadata.anonymous.toPosition
import gg.tropic.practice.map.metadata.impl.MapSpawnMetadata
import gg.tropic.practice.map.metadata.scanner.AbstractMapMetadataScanner
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel
import org.bukkit.block.BlockFace
import org.bukkit.material.Sign

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
object MapSpawnMetadataScanner : AbstractMapMetadataScanner<MapSpawnMetadata>()
{
    override val type = "spawn"

    private val manualMappings = mutableMapOf(
        BlockFace.NORTH to 180.0F,
        BlockFace.WEST to 90.0F,
        BlockFace.SOUTH to 0.0F,
        BlockFace.EAST to -90.0F,

        BlockFace.SOUTH_WEST to 45.0F,
        BlockFace.SOUTH_EAST to -45.0F,
        BlockFace.NORTH_WEST to 135.0F,
        BlockFace.NORTH_EAST to -135.0F
    )

    override fun scan(
        id: String, models: List<MapSignMetadataModel>
    ): MapSpawnMetadata
    {
        val model = models.first()
        val location = model.location.clone()

        val sign = model.location.block.state.data as Sign
        location.yaw = manualMappings[sign.facing]!!

        location.z += 0.500F
        location.x += 0.500F

        return MapSpawnMetadata(id, location.toPosition())
    }
}
