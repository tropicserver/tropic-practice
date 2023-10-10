package gg.tropic.practice.replications.models

/**
 * @author GrowlyX
 * @since 10/9/2023
 */
data class Replication(
    val server: String,
    val associatedMapName: String,
    val name: String,
    val inUse: Boolean = false
)
