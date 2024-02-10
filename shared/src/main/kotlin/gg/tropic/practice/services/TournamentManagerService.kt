package gg.tropic.practice.services

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import gg.tropic.practice.tournaments.TournamentMemberList
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 12/25/2023
 */
@Service
object TournamentManagerService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:tournaments".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private var tournamentMembers = mutableListOf<UUID>()

    fun isInTournament(uniqueId: UUID) = uniqueId in tournamentMembers

    @Configure
    fun configure()
    {
        aware.connect()

        Schedulers
            .async()
            .runRepeating({ _ ->
                val members = ScalaCommonsSpigot.instance
                    .kvConnection
                    .sync()
                    .get("${namespace().suffixWhenDev()}:tournaments:members")

                tournamentMembers = if (members != null)
                {
                    Serializers.gson
                        .fromJson(members, TournamentMemberList::class.java)
                        .members
                        .toMutableList()
                } else
                {
                    mutableListOf()
                }
            }, 0L, 1L)
    }

    fun publish(
        id: String,
        vararg data: Pair<String, Any>
    ) = CompletableFuture
        .runAsync {
            AwareMessage
                .of(
                    packet = id,
                    aware,
                    *data
                )
                .publish(
                    AwareThreadContext.SYNC
                )
        }
}
