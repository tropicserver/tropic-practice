package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
data class MapLevelMetadata(
    val yAxis: Int,
    val below: Int = 2,
    val allowBuildOnBlockSideBlockFaces: Boolean = false,
    override val id: String = "level",
) : AbstractMapMetadata()
{
    val range: IntRange
        get() = (yAxis - below)..yAxis

    override fun getAbstractType() = MapLevelMetadata::class.java
}
