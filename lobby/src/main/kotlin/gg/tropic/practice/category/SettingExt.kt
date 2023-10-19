package gg.tropic.practice.category

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.tropic.practice.category.restriction.RangeRestriction
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/15/2023
 */
val Player.pingRange: RangeRestriction
    get() = BasicsProfileService.find(this)
        ?.setting<RangeRestriction>(
            "duels:ranked-restriction-ping",
            RangeRestriction.None
        )
        ?: RangeRestriction.None
