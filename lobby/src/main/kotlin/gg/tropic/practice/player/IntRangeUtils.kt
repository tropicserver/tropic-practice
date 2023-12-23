package gg.tropic.practice.player

import net.evilblock.cubed.util.bukkit.Constants

/**
 * @author GrowlyX
 * @since 12/23/2023
 */
fun IntRange.formattedDomain() = "[${kotlin.math.max(0, first)} ${Constants.ARROW_RIGHT} $last]"
