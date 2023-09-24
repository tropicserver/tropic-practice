package gg.tropic.practice.application.api.defaults.kit.group

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class ImmutableKitGroup(
    val id: String,
    val contains: MutableList<String> = mutableListOf()
)
