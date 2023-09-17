package gg.tropic.practice.arena

import com.grinderwolf.swm.api.world.properties.SlimeProperties
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import gg.tropic.practice.kit.DuelLadder

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
data class Arena(
    val uniqueId: String,
    val display: String,
    val arenaFile: String,
    val spawns: Map<Int, ArenaLocation> =
        mutableMapOf(),
    val compatible: List<DuelLadder> =
        DuelLadder.entries,
    val properties: SlimePropertyMap =
        SlimePropertyMap().apply {
            setString(SlimeProperties.DIFFICULTY, "normal")
            setInt(SlimeProperties.SPAWN_X, 0)
            setInt(SlimeProperties.SPAWN_Y, 100)
            setInt(SlimeProperties.SPAWN_Z, 0)
        }
)
