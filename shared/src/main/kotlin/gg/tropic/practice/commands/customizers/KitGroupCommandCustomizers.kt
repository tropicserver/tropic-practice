package gg.tropic.practice.commands.customizers

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import gg.tropic.practice.map.Map
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
object KitGroupCommandCustomizers
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
            .registerAsyncCompletion("kit-groups") {
                KitGroupService.cached()
                    .groups.map(KitGroup::id)
            }

        manager.commandCompletions
            .registerAsyncCompletion("stranger-kit-groups") {
                val map = it.getContextValue(Map::class.java)

                KitGroupService.cached()
                    .groups.map(KitGroup::id)
                    .filterNot { group ->
                        group in map.associatedKitGroups
                    }
            }

        manager.commandCompletions
            .registerAsyncCompletion("associated-kit-groups") {
                val map = it.getContextValue(Map::class.java)

                KitGroupService.cached()
                    .groups.map(KitGroup::id)
                    .filter { group ->
                        group in map.associatedKitGroups
                    }
            }
    }
}
