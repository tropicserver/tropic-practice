package gg.tropic.practice.map.metadata.sign

import gg.tropic.practice.map.metadata.scanner.AbstractMapMetadataScanner
import org.bukkit.Location

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapSignMetadataModel(
    val metaType: String,
    val location: Location,
    val id: String,
    val extraMetadata: List<String>,
    val scanner: AbstractMapMetadataScanner<*>
)
{
    fun flags(id: String) = extraMetadata
        .any {
            it == id
        }

    fun valueOf(id: String) = extraMetadata
        .firstOrNull {
            it.startsWith("$id=")
        }
        ?.split("=")?.first()
}
