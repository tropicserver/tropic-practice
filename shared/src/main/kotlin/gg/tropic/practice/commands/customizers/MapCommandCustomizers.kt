package gg.tropic.practice.commands.customizers

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
object MapCommandCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandContexts.registerContext(Map::class.java) {
            val arg = it.popFirstArg()

            MapService.cached().maps.values
                .firstOrNull { group ->
                    group.name.equals(arg, true)
                }
                ?: throw ConditionFailedException(
                    "No map with the ID ${CC.YELLOW}$arg${CC.RED} exists."
                )
        }

        manager.commandCompletions
            .registerCompletion("maps") {
                MapService.cached().maps.keys
            }
    }
}
