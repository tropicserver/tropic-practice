package gg.tropic.practice.settings.particles

import com.google.common.collect.ImmutableMap
import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.entity.Player
import xyz.xenondevs.particle.ParticlePacket
import xyz.xenondevs.particle.ParticleEffect

val chatColorToAwtColorMappings: Map<ChatColor, Color> = ImmutableMap.builder<ChatColor, Color>()
    .put(ChatColor.BLACK, Color.fromRGB(0, 0, 0))
    .put(ChatColor.DARK_BLUE, Color.fromRGB(0, 0, 170))
    .put(ChatColor.DARK_GREEN, Color.fromRGB(0, 170, 0))
    .put(ChatColor.DARK_AQUA, Color.fromRGB(0, 170, 170))
    .put(ChatColor.DARK_RED, Color.fromRGB(170, 0, 0))
    .put(ChatColor.DARK_PURPLE, Color.fromRGB(170, 0, 170))
    .put(ChatColor.GOLD, Color.fromRGB(255, 170, 0))
    .put(ChatColor.GRAY, Color.fromRGB(170, 170, 170))
    .put(ChatColor.DARK_GRAY, Color.fromRGB(85, 85, 85))
    .put(ChatColor.BLUE, Color.fromRGB(85, 85, 255))
    .put(ChatColor.GREEN, Color.fromRGB(85, 255, 85))
    .put(ChatColor.AQUA, Color.fromRGB(85, 255, 255))
    .put(ChatColor.RED, Color.fromRGB(255, 85, 85))
    .put(ChatColor.LIGHT_PURPLE, Color.fromRGB(255, 85, 255))
    .put(ChatColor.YELLOW, Color.fromRGB(255, 255, 85))
    .put(ChatColor.WHITE, Color.fromRGB(255, 255, 255))
    .build()

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
enum class FlightEffectSetting(
    private val display: String? = null,
    val createPacket: () -> ParticlePacket,
    val multiplied: Boolean = false
) : SettingValue
{
    /*Rainbow(
        display = LegacyComponentSerializer
            .legacySection()
            .serialize(
                MiniMessage
                    .miniMessage()
                    .deserialize("<rainbow>Rainbow</rainbow>")
            ),
        createPacket = {
            TODO()
        }
    ),*/
    Sparks(
        display = "Sparks",
        createPacket = {
            ParticlePacket(
                ParticleEffect.FIREWORKS_SPARK,
                0.15f,
                0.15f,
                0.15f,
                0f, 8,
                null
            )
        }
    ),
    Fire(
        display = "Fire",
        createPacket = {
            ParticlePacket(
                ParticleEffect.FLAME,
                0.15f,
                0.15f,
                0.15f,
                0f, 8,
                null
            )
        },
        multiplied = true
    ),
    Cloud(
        display = "Cloud",
        createPacket = {
            ParticlePacket(
                ParticleEffect.CLOUD,
                0.15f,
                0.15f,
                0.15f,
                0f, 8,
                null
            )
        }
    ),
    WaterBubbles(
        display = "Water Bubbles",
        createPacket = {
            ParticlePacket(
                ParticleEffect.DRIP_WATER,
                0.15f,
                0.15f,
                0.15f,
                0f, 8,
                null
            )
        },
        multiplied = true
    ),
    Villager(
        display = "Villager",
        createPacket = {
            ParticlePacket(
                ParticleEffect.VILLAGER_HAPPY,
                0.15f,
                0.15f,
                0.15f,
                0f, 8,
                null
            )
        }
    ),
    None(
        createPacket = {
            TODO()
        }
    );

    override val displayName = display ?: name
    override fun display(player: Player) = true
}
