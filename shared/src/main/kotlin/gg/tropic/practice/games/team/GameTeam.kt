package gg.tropic.practice.games.team

import org.bukkit.Bukkit
import java.util.UUID

/**
 * @author GrowlyX
 * @since 8/9/2022
 */
class GameTeam(
    val side: GameTeamSide,
    val players: List<UUID>
)
{
    @Volatile
    var combinedHits = 0

    private var backingPlayerCombos: MutableMap<UUID, Int>? = null
        get()
        {
            if (field == null)
            {
                field = mutableMapOf()
            }
            return field
        }

    private var backingHighestPlayerCombos: MutableMap<UUID, Int>? = null
        get()
        {
            if (field == null)
            {
                field = mutableMapOf()
            }
            return field
        }

    val playerCombos: MutableMap<UUID, Int>
        get() = backingPlayerCombos!!

    val highestPlayerCombos: MutableMap<UUID, Int>
        get() = backingHighestPlayerCombos!!

    fun nonSpectators() = this.toBukkitPlayers()
        .filterNotNull()
        .filter {
            !it.hasMetadata("spectator")
        }

    fun toBukkitPlayers() = this.players
        .map {
            Bukkit.getPlayer(it) ?: null
        }
}
