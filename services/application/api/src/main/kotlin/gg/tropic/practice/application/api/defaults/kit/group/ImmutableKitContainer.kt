package gg.tropic.practice.application.api.defaults.kit.group

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class ImmutableKitContainer(
    val backingGroups: List<ImmutableKitGroup> = listOf()
)
