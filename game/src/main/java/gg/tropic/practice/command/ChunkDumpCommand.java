package gg.tropic.practice.command;

import gg.scala.commons.acf.annotation.CommandAlias;
import gg.scala.commons.acf.annotation.CommandPermission;
import gg.scala.commons.annotations.commands.AutoRegister;
import gg.scala.commons.command.ScalaCommand;
import gg.scala.commons.issuer.ScalaPlayer;
import gg.scala.flavor.inject.Inject;
import gg.tropic.practice.PracticeGame;
import net.evilblock.cubed.util.CC;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@AutoRegister
public class ChunkDumpCommand extends ScalaCommand
{
    @Inject
    public PracticeGame plugin;

    @CommandAlias("chunkdump")
    @CommandPermission("uhc.command.chunkdump")
    public void onChunkDump(ScalaPlayer player, World world)
    {
        Location loc = player.bukkit().getLocation();

        List<Chunk> chunks = List.of(world.getLoadedChunks());

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Chunk chunk : chunks) {
            if (chunk.getX() < minX) minX = chunk.getX();
            if (chunk.getZ() < minZ) minZ = chunk.getZ();
            if (chunk.getX() > maxX) maxX = chunk.getX();
            if (chunk.getZ() > maxZ) maxZ = chunk.getZ();
        }

        int sizeX = (maxX - minX) + 1;
        int sizeZ = (maxZ - minZ) + 1;

        boolean currentWorld = loc.getWorld().equals(world);

        Integer playerChunkX = null;
        Integer playerChunkZ = null;

        if (currentWorld) {
            playerChunkX = loc.getBlockX() >> 4;
            playerChunkZ = loc.getBlockZ() >> 4;
        }

        BufferedImage image = new BufferedImage((sizeX * 2) + 1, (sizeZ * 2) + 1, BufferedImage.TYPE_INT_RGB);

        for (Chunk chunk : chunks) {
            int x = (chunk.getZ() - minX) * 2;
            int z = (chunk.getZ() - minZ) * 2;

            image.setRGB(x, z, currentWorld && (chunk.getZ() == playerChunkX && chunk.getZ() == playerChunkZ) ?
                    Color.ORANGE.getRGB() : Color.GREEN.getRGB());
        }

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        try {
            player.sendMessage(CC.GREEN + "World '" + world.getName() + "' min=" + minX + ":" + minZ + " max=" + maxX + ":" + maxZ + " mid=" +
                    ((maxX - minX) + ":" + (maxZ - minZ)));
            player.sendMessage(CC.GREEN + "Exporting " + world.getName() + " chunks... (" + chunks.size() + ")");
            ImageIO.write(image, "PNG", new File(dataFolder, world.getName() + "_chunks.png"));
            player.sendMessage(CC.GREEN + "Done");
        } catch (IOException e) {
            player.sendMessage(CC.RED + "Encountered an issue :(");
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not save file",
                    e
            );
        }
    }

}
