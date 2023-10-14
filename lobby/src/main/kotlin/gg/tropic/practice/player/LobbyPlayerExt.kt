package gg.tropic.practice.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
val LobbyPlayer.player: Player
    get() = Bukkit.getPlayer(uniqueId)

val String.component: Component
    get() = LegacyComponentSerializer
        .legacySection()
        .deserialize(this)
