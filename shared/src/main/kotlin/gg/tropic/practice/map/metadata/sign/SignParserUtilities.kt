package gg.tropic.practice.map.metadata.sign

import gg.tropic.practice.map.metadata.scanner.MetadataScannerUtilities
import org.bukkit.Location
import java.util.LinkedList

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
fun List<String>.parseIntoMetadata(location: Location): MapSignMetadataModel?
{
    if (size < 2)
    {
        return null
    }

    val type = first()

    if (
        !type.startsWith("[") ||
        !type.endsWith("]")
    )
    {
        return null
    }

    val typeDelimited = type
        .removePrefix("[")
        .removeSuffix("]")

    val scanner = MetadataScannerUtilities
        .matches(typeDelimited)
        ?: return null

    val linked = LinkedList(this)
    linked.pop()

    return MapSignMetadataModel(
        metaType = typeDelimited,
        id = linked.pop(),
        extraMetadata = linked.toList(),
        scanner = scanner,
        location = location
    )
}
