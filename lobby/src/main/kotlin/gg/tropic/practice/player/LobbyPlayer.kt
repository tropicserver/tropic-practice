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
    var leaveQueueOnLogout = true
    var maintainStateTimeout = -1L

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

    fun queueState() = queueState!!

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

        // keep current state until the server processes our queue join
        // request and actually updates the queue entry
        if (maintainStateTimeout > System.currentTimeMillis())
        {
            // if we notice that the server pushed whatever state we're expecting (whether it's a queue
            // join/leave that we set temporarily), we'll remove the timeout and continue with our day.
            if (newState == state)
            {
                maintainStateTimeout = -1L
            } else
            {
                return
            }
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
