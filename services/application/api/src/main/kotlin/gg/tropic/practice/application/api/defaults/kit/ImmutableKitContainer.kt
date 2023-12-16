package gg.tropic.practice.application.api.defaults.kit

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class ImmutableKitContainer(
    val kits: Map<String, ImmutableKit> = mapOf()
)
