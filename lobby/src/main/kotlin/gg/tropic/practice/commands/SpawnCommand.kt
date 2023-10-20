package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Conditions
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.configuration.LobbyConfigurationService
import org.bukkit.Bukkit

/**
 * @author GrowlyX
 * @since 10/19/2023
 */
@AutoRegister
object SpawnCommand : ScalaCommand()
{
    @CommandAlias("spawn")
    fun onSpawn(
        @Conditions("cooldown:duration=2,unit=SECONDS")
        player: ScalaPlayer
    )
    {
        with(LobbyConfigurationService.cached()) {
            player.teleport(
                spawnLocation
                    .toLocation(
                        Bukkit.getWorlds().first()
                    )
            )
        }
    }
}
