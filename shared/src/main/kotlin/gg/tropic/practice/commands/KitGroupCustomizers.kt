package gg.tropic.practice.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
object KitGroupCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandContexts.registerContext(KitGroup::class.java) {
            val arg = it.popFirstArg()

            KitGroupService.cached().groups
                .firstOrNull { group ->
                    group.id.equals(arg, true)
                }
                ?: throw ConditionFailedException(
                    "No kit group with the ID ${CC.YELLOW}$arg${CC.RED} exists."
                )
        }

        manager.commandCompletions
            .registerCompletion("kit-groups") {
                KitGroupService.cached()
                    .groups.map(KitGroup::id)
            }
    }
}
