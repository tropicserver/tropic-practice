package gg.tropic.practice.services

import gg.scala.commons.scoreboard.TextAnimator
import gg.scala.commons.scoreboard.animations.TextFadeAnimation
import gg.scala.flavor.service.Close
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
@Service
object ScoreboardTitleService
{
    private val titleAnimator = TextAnimator.of(
        TextFadeAnimation(
            "${CC.BOLD}Practice",
            ChatColor.AQUA,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_GRAY
        )
    )

    fun getCurrentTitle() = titleAnimator.current().text

    @Configure
    fun configure()
    {
        titleAnimator.schedule()
    }

    @Close
    fun close()
    {
        titleAnimator.dispose()
    }
}
