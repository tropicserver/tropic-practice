package gg.tropic.practice.category

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.tropic.practice.settings.restriction.RangeRestriction
import gg.tropic.practice.settings.DuelsSettingCategory
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/15/2023
 */
val Player.pingRange: RangeRestriction
    get() = BasicsProfileService.find(this)
        ?.setting<RangeRestriction>(
            "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:restriction-ping",
            RangeRestriction.None
        )
        ?: RangeRestriction.None
