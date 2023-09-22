package gg.tropic.practice.commands.customizers

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
object KitCommandCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandContexts.registerContext(Kit::class.java) {
            val arg = it.popFirstArg()

            KitService.cached().kits[arg]
                ?: throw ConditionFailedException(
                    "No kit with the ID ${CC.YELLOW}$arg${CC.RED} exists."
                )
        }

        manager.commandCompletions
            .registerCompletion("kits") {
                KitService.cached().kits.keys
            }
    }
}
