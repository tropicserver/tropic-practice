package gg.tropic.practice.commands.customizers

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC
import org.bukkit.potion.PotionEffectType

/**
 * @author GrowlyX
 * @since 9/22/2023
 */
object KitCommandCustomizers
{
    private val potionEffectRegistry = """
            SPEED
            SLOW
            FAST_DIGGING
            SLOW_DIGGING
            INCREASE_DAMAGE
            HEAL
            HARM
            JUMP
            CONFUSION
            REGENERATION
            DAMAGE_RESISTANCE
            FIRE_RESISTANCE
            WATER_BREATHING
            INVISIBILITY
            BLINDNESS
            NIGHT_VISION
            HUNGER
            WEAKNESS
            POISON
            WITHER
            HEALTH_BOOST
            ABSORPTION 
            SATURATION 
        """.trimIndent()
        .split("\n")
        .associateWith { PotionEffectType.getByName(it) }

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
            .registerAsyncCompletion("kits") {
                KitService.cached().kits.keys
            }

        manager.commandCompletions
            .registerAsyncCompletion("stranger-feature-flags-schemakeys") {
                val kit = it.getContextValue(Kit::class.java)
                val flag = it.getContextValue(FeatureFlag::class.java)
                val entry = kit.features[flag] ?: emptyMap()

                flag.schema.keys
                    .filterNot { key ->
                        key in entry.keys
                    }
            }

        manager.commandCompletions
            .registerAsyncCompletion("existing-feature-flags-schemakeys") {
                val kit = it.getContextValue(Kit::class.java)
                val flag = it.getContextValue(FeatureFlag::class.java)
                val entry = kit.features[flag] ?: emptyMap()

                flag.schema.keys
                    .filter { key ->
                        key in entry.keys
                    }
            }

        manager.commandCompletions
            .registerAsyncCompletion("stranger-feature-flags") {
                val kit = it.getContextValue(Kit::class.java)

                FeatureFlag.entries
                    .filterNot { flag ->
                        flag in kit.features
                    }
                    .map(FeatureFlag::name)
            }

        manager.commandCompletions
            .registerAsyncCompletion("existing-feature-flags") {
                val kit = it.getContextValue(Kit::class.java)

                FeatureFlag.entries
                    .filter { flag ->
                        flag in kit.features
                    }
                    .map(FeatureFlag::name)
            }

        manager.commandCompletions
            .registerAsyncCompletion("stranger-kit-groups-to-kit") {
                val kit = it.getContextValue(Kit::class.java)

                KitGroupService.cached()
                    .groups
                    .filterNot { group ->
                        kit.id in group.contains
                    }
                    .map(KitGroup::id)
            }

        manager.commandCompletions
            .registerAsyncCompletion(
                "effects"
            ) {
                potionEffectRegistry.keys
            }

        manager.commandContexts
            .registerContext(
                PotionEffectType::class.java
            ) {
                val arg = it.popFirstArg()

                potionEffectRegistry[arg]
                    ?: throw ConditionFailedException(
                        "No potion effect with the ID $arg exists."
                    )
            }

        manager.commandCompletions
            .registerAsyncCompletion("associated-kit-groups-with-kit") {
                val kit = it.getContextValue(Kit::class.java)

                KitGroupService.cached()
                    .groups
                    .filter { group ->
                        kit.id in group.contains
                    }
                    .map(KitGroup::id)
            }
    }
}
