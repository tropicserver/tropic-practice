package gg.tropic.practice.replications

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class Replication(
    val server: String,
    val associatedMapName: String,
    val name: String,
    val inUse: Boolean = false
)

data class ReplicationStatus(
    val replications: Map<String, List<Replication>>
)
