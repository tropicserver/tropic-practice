package gg.tropic.practice.commands;

import gg.scala.commons.acf.ConditionFailedException;
import gg.scala.commons.acf.annotation.CommandAlias;
import gg.scala.commons.acf.annotation.Optional;
import gg.scala.commons.agnostic.sync.server.ServerContainer;
import gg.scala.commons.agnostic.sync.server.impl.GameServer;
import gg.scala.commons.annotations.commands.AutoRegister;
import gg.scala.commons.command.ScalaCommand;
import gg.scala.commons.issuer.ScalaPlayer;
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem;
import net.evilblock.cubed.menu.Button;
import net.evilblock.cubed.menu.pagination.PaginatedMenu;
import net.evilblock.cubed.util.CC;
import net.evilblock.cubed.util.bukkit.ColorUtil;
import net.evilblock.cubed.util.bukkit.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GrowlyX
 * @since 4/2/2023
 */
@AutoRegister
public class SwitchLobbyServerCommand extends ScalaCommand {
    private static class SwitchLobbyServerMenu extends PaginatedMenu {
        public SwitchLobbyServerMenu() {
            setAutoUpdate(true);
        }

        public static void redirectToServer(
                @NotNull Player player, @NotNull GameServer gameServer
        ) {
            if (gameServer.getWhitelisted()) {
                player.sendMessage(CC.RED + "Server is whitelisted!");
                return;
            }

            if (gameServer.getPlayersCount() >= gameServer.getMaxPlayers()) {
                player.sendMessage(CC.RED + "Server is full!");
                return;
            }

            player.closeInventory();

            VelocityRedirectSystem.INSTANCE
                    .redirect(player, gameServer.getId());
        }

        @NotNull
        @Override
        public Map<Integer, Button> getAllPagesButtons(@NotNull Player player) {
            var buttons = new HashMap<Integer, Button>();

            ServerContainer.INSTANCE
                    .getServersInGroup("miplobby")
                    .stream()
                    .sorted(Comparator.comparing(server ->
                            Integer.valueOf(
                                    server.getId().replace("practice-lobby", "")
                            )
                    ))
                    .forEach(server -> {
                        var gameServer = (GameServer) server;

                        buttons.put(
                                buttons.size(),
                                ItemBuilder
                                        .of(Material.WOOL)
                                        .data(
                                                (short) ColorUtil.toWoolData(
                                                        gameServer.getWhitelisted() ?
                                                                ChatColor.RED :
                                                                gameServer.getPlayersCount() >= gameServer.getMaxPlayers() ?
                                                                        ChatColor.GRAY :
                                                                        ChatColor.GREEN
                                                )
                                        )
                                        .name(CC.GREEN + server.getId())
                                        .addToLore(
                                                CC.GRAY + gameServer.getPlayersCount() + "/" + gameServer.getMaxPlayers() + " online...",
                                                "",
                                                (
                                                        gameServer.getWhitelisted() ?
                                                                ChatColor.RED + "Server is whitelisted!" :
                                                                gameServer.getPlayersCount() >= gameServer.getMaxPlayers() ?
                                                                        ChatColor.RED + "Server is full!" :
                                                                        ChatColor.YELLOW + "Click to join!"
                                                )
                                        )
                                        .toButton((player1, clickType) ->
                                                redirectToServer(player, gameServer)
                                        )
                        );
                    });

            return buttons;
        }

        @NotNull
        @Override
        public String getPrePaginatedTitle(@NotNull Player player) {
            return "Switch Pokemon Server";
        }
    }

    @CommandAlias("switchlobby|switchserver")
    public void onSwitchLobby(ScalaPlayer player, @Optional Integer server) {
        if (server != null) {
            if (server < 1) {
                throw new ConditionFailedException("Please enter a server id greater than or equal to 1!");
            }

            var gameServer = (GameServer) ServerContainer
                    .INSTANCE.getServer("practice-lobby" + server);

            if (gameServer == null) {
                throw new ConditionFailedException("The pokemon server " + server + " is not online!");
            }

            SwitchLobbyServerMenu.redirectToServer(player.bukkit(), gameServer);
            return;
        }

        new SwitchLobbyServerMenu().openMenu(player.bukkit());
    }
}
