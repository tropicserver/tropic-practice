package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.agnostic.sync.server.ServerContainer.getServersInGroup
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ColorUtil.toWoolData
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 4/2/2023
 */
@AutoRegister
object SwitchLobbyServerCommand : ScalaCommand()
{
    private class SwitchLobbyServerMenu : PaginatedMenu()
    {
        init
        {
            autoUpdate = true
        }

        override fun getAllPagesButtons(player: Player): Map<Int, Button>
        {
            val buttons = mutableMapOf<Int, Button>()

            getServersInGroup("miplobby")
                .sortedBy {
                    extractNumbers(it.id)
                }
                .filterIsInstance<GameServer>()
                .forEach { gameServer ->
                    buttons[buttons.size] = ItemBuilder.of(Material.WOOL)
                        .data(
                            toWoolData(
                                if (gameServer.getWhitelisted()!!)
                                    ChatColor.RED
                                else
                                    if (gameServer.getPlayersCount()!! >= gameServer.getMaxPlayers()!!)
                                        ChatColor.GRAY
                                    else
                                        ChatColor.GREEN
                            ).toShort()
                        )
                        .name(CC.GREEN + gameServer.id)
                        .addToLore(
                            CC.GRAY + gameServer.getPlayersCount() + "/" + gameServer.getMaxPlayers() + " online...",
                            "",
                            (if (gameServer.getWhitelisted()!!)
                                ChatColor.RED.toString() + "Server is whitelisted!"
                            else
                                if (gameServer.getPlayersCount()!! >= gameServer.getMaxPlayers()!!)
                                    ChatColor.RED.toString() + "Server is full!"
                                else
                                    ChatColor.YELLOW.toString() + "Click to join!")
                        )
                        .toButton { _, _ ->
                            redirectToServer(
                                player,
                                gameServer
                            )
                        }
                }

            return buttons
        }

        override fun getPrePaginatedTitle(player: Player) = "Switch Lobby Server"

        fun redirectToServer(
            player: Player, gameServer: GameServer
        )
        {
            if (gameServer.getWhitelisted()!!)
            {
                player.sendMessage("${CC.RED}Server is whitelisted!")
                return
            }

            if (gameServer.getPlayersCount()!! >= gameServer.getMaxPlayers()!!)
            {
                player.sendMessage("${CC.RED}Server is full!")
                return
            }

            player.closeInventory()

            VelocityRedirectSystem
                .redirect(player, gameServer.id)
        }
    }

    @CommandAlias("switchlobby|switchserver")
    fun onSwitchLobby(player: ScalaPlayer)
    {
        SwitchLobbyServerMenu().openMenu(player.bukkit())
    }
}
