package gg.tropic.practice.player

import gg.tropic.practice.kit.KitService
import gg.tropic.practice.player.hotbar.LobbyHotbarService
import gg.tropic.practice.queue.QueueState
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import org.bukkit.Bukkit
import java.util.UUID

data class LobbyPlayer(
    val uniqueId: UUID
)
{
    val stateUpdateLock = Any()

    var state: PlayerState = PlayerState.None
        set(value)
        {
            field = value

            if (field != PlayerState.None)
            {
                val bukkit = Bukkit.getPlayer(uniqueId)!!
                LobbyHotbarService
                    .get(value)
                    .applyToPlayer(bukkit)
            }
        }

    private var queueState: QueueState? = null
    fun buildQueueID() = queueState?.let {
        "${it.kitId}:${it.queueType.name}:${it.teamSize}v${it.teamSize}"
    }

    fun inQueue() = queueState != null
    fun queuedForTime() = System.currentTimeMillis() - queueState!!.joined
    fun queuedForKit() = KitService.cached().kits[queueState!!.kitId]
    fun queuedForType() = queueState!!.queueType
    fun queuedForTeamSize() = queueState!!.teamSize

    fun syncQueueState()
    {
        queueState = ScalaCommonsSpigot.instance.kvConnection
            .sync()
            .hget(
                "tropicpractice:queue-states",
                uniqueId.toString()
            )
            ?.let {
                Serializers
                    .gson
                    .fromJson(it, QueueState::class.java)
            }

        val newState = if (queueState != null)
        {
            PlayerState.InQueue
        } else
        {
            PlayerState.Idle
        }

        // don't try to acquire lock if we don't need to
        if (newState != state)
        {
            synchronized(stateUpdateLock) {
                // check if the state has changed before we acquired the lock
                if (newState != state)
                {
                    state = newState
                }
            }
        }
    }
}
