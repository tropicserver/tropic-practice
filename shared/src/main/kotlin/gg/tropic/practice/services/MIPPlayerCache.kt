package gg.tropic.practice.services

import com.google.gson.reflect.TypeToken
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.Lemon
import gg.scala.lemon.handler.PlayerHandler
import gg.scala.lemon.util.QuickAccess
import me.lucko.helper.Schedulers
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import org.bukkit.Bukkit

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
@Service
object MIPPlayerCache
{
    private var playerIDs = setOf<String>()

    data class Player(val username: String)

    private var localModelCache = listOf<Player>()

    @Configure
    fun configure()
    {
        val cache = ScalaCommonsSpigot.instance.kvConnection

        Schedulers
            .async()
            .runRepeating({ _ ->
                val localModels = mutableListOf<Player>()

                Bukkit.getOnlinePlayers()
                    .sortedByDescending {
                        QuickAccess.realRank(it).weight
                    }
                    .forEach {
                        PlayerHandler
                            .find(it.uniqueId)
                            ?: return@forEach

                        if (it.hasMetadata("vanished"))
                        {
                            return@forEach
                        }

                        localModels += Player(it.name)
                    }

                cache.sync().hset(
                    "tropicpractice:player-sync",
                    Lemon.instance.settings.id,
                    Serializers.gson.toJson(localModels)
                )
            }, 0L, 3L)

        val typeToken = object : TypeToken<List<Player>>()
        {}.type

        Schedulers
            .async()
            .runRepeating({ _ ->
                val mappings = cache.sync()
                    .hgetall("tropicpractice:player-sync")
                    .let {
                        it
                            .filterKeys { k -> ServerContainer.getServer(k) != null }
                            .mapValues { v ->
                                Serializers.gson
                                    .fromJson<List<Player>>(v.value, typeToken)
                            }
                    }

                localModelCache = mappings.values.flatten().toList()
                playerIDs = localModelCache.map { it.username }.toSet()
            }, 0L, 5)
    }

    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandCompletions.registerCompletion("mip-players") { playerIDs }
    }
}
