package gg.tropic.practice.utilities;

import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.WorldType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static xyz.xenondevs.particle.utils.ReflectionUtils.sendPacket;

/**
 * @author GrowlyX
 * @since 12/24/2023
 */
public class PlayerRespawnUtilities {

    public static void mockPlayerRespawn(@NotNull final Player player) {
        final byte actualDimension = (byte) player.getWorld().getEnvironment().getId();
        final EnumDifficulty difficulty = switch (player.getWorld().getDifficulty()) {
            case PEACEFUL -> EnumDifficulty.PEACEFUL;
            case EASY -> EnumDifficulty.EASY;
            case HARD -> EnumDifficulty.HARD;
            default -> EnumDifficulty.NORMAL;
        };
        final WorldType worldType = switch (player.getWorld().getWorldType()) {
            case NORMAL -> WorldType.NORMAL;
            case VERSION_1_1 -> WorldType.NORMAL_1_1;
            case LARGE_BIOMES -> WorldType.LARGE_BIOMES;
            case AMPLIFIED -> WorldType.AMPLIFIED;
            default -> WorldType.FLAT;
        };
        final WorldSettings.EnumGamemode gameMode = switch (player.getGameMode()) {
            case CREATIVE -> WorldSettings.EnumGamemode.CREATIVE;
            case ADVENTURE -> WorldSettings.EnumGamemode.ADVENTURE;
            default -> WorldSettings.EnumGamemode.SURVIVAL;
        };

        final Location location = player.getLocation();
        sendPacket(
                player,
                new PacketPlayOutRespawn(
                        actualDimension, difficulty,
                        worldType, gameMode
                )
        );
        player.teleport(location);
    }
}
