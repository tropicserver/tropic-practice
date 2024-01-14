package gg.tropic.practice.feature

import com.lunarclient.apollo.Apollo
import com.lunarclient.apollo.module.combat.CombatModule
import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@Service
@IgnoreAutoScan
@SoftDependency("Apollo-Bukkit")
object ApolloFeature
{
    @Configure
    fun configure()
    {
        Apollo.getModuleManager()
            .getModule(CombatModule::class.java)
            .options.set(
                CombatModule.DISABLE_MISS_PENALTY, true
            )
    }
}
