package gg.tropic.practice.application.api.defaults.map

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class ImmutableMap(
    val name: String,
    val displayName: String,
    val associatedSlimeTemplate: String,
    val associatedKitGroups: Set<String>,
    val locked: Boolean
)
