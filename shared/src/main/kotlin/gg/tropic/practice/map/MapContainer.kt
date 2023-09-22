package gg.tropic.practice.map

import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
data class MapContainer(val maps: MutableMap<UUID, Map> = mutableMapOf())
