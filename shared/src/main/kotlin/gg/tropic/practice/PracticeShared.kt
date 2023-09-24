package gg.tropic.practice

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.redirection.aggregate.impl.LeastTrafficServerAggregateHandler
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.expectation.DuelExpectation
import gg.tropic.practice.games.AbstractGame
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import me.lucko.helper.Schedulers
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
object PracticeShared
{
    const val KEY = "tropicpractice"

    private lateinit var redirector: ServerAggregateHandler

    // i don't like this, but we need to do it
    init
    {
        Serializers.create {
            registerTypeAdapter(
                AbstractMapMetadata::class.java,
                AbstractTypeSerializer<AbstractMapMetadata>()
            )

            registerTypeAdapter(
                PotionEffectType::class.java,
                object : TypeAdapter<PotionEffectType>()
                {
                    override fun write(out: JsonWriter?, value: PotionEffectType?)
                    {
                        out?.value(value?.name)
                    }

                    override fun read(`in`: JsonReader?): PotionEffectType?
                    {
                        return PotionEffectType.getByName(`in`?.nextString())
                    }
                }
            )
        }
    }

    fun load()
    {
        DataStoreObjectControllerCache.create<DuelExpectation>()
        DataStoreObjectControllerCache.create<AbstractGame>()

        redirector = LeastTrafficServerAggregateHandler("duels")
        redirector.subscribe()
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

        val bestServer = redirector
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

        // TODO: rework. pregen map and then send a packet
        //  to push all players to the servers.
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
