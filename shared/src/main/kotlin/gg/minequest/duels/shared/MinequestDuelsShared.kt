package gg.minequest.duels.shared

import gg.minequest.duels.shared.expectation.DuelExpectation
import gg.minequest.duels.shared.game.AbstractGame
import gg.minequest.duels.shared.game.GameReport
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.redirection.aggregate.impl.LeastTrafficServerAggregateHandler
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import java.util.LinkedList

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
object MinequestDuelsShared
{
    private lateinit var redirector: ServerAggregateHandler

    fun load()
    {
        DataStoreObjectControllerCache.create<DuelExpectation>()
        DataStoreObjectControllerCache.create<GameReport>()

        DataStoreObjectControllerCache.create<AbstractGame>()

        this.redirector = LeastTrafficServerAggregateHandler("duels")
        this.redirector.subscribe()
    }

    fun initiateDuel(
        players: List<Player>,
        expectation: DuelExpectation
    )
    {
        DataStoreObjectControllerCache
            .findNotNull<DuelExpectation>()
            .save(expectation, DataStoreStorageType.REDIS)
            .join()

        val bestServer = this.redirector
            .findBestChoice(players.first())
            ?: return kotlin.run {
                DataStoreObjectControllerCache
                    .findNotNull<DuelExpectation>()
                    .delete(
                        expectation.identifier,
                        DataStoreStorageType.REDIS
                    )

                players.forEach {
                    it.sendMessage("${CC.RED}We were unable to send you to a server!")
                }
            }

        val playersLinked = LinkedList(players)

        Schedulers.sync().runRepeating({ task ->
            if (playersLinked.isEmpty())
            {
                task.closeAndReportException()
                return@runRepeating
            }

            VelocityRedirectSystem.redirect(
                playersLinked.pop(), bestServer.id
            )
        }, 0L, 20L)
    }
}
