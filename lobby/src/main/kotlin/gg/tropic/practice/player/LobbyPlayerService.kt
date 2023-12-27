package gg.tropic.practice.player

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.commands.TournamentCommand
import gg.tropic.practice.configuration.LobbyConfigurationService
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.serializable.Message
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.settings.particles.FlightEffectSetting
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.utils.Players
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.Color
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.time.TimeUtil
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import xyz.xenondevs.particle.utils.ReflectionUtils
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

@Service
object LobbyPlayerService
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Inject
    lateinit var audiences: BukkitAudiences

    private val playerCache = mutableMapOf<UUID, LobbyPlayer>()

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:lobbies")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    private fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                Players.all()
                    .mapNotNull(::find)
                    .onEach(LobbyPlayer::syncQueueState)
                    .filter(LobbyPlayer::inQueue)
                    .onEach {
                        val audience = audiences.player(it.player)

                        val shouldIncludePingRange = it.validateQueueEntry() &&
                            it.queueState().queueType == QueueType.Ranked &&
                            it.queueEntry().maxPingDiff != -1

                        val shouldIncludeELORange = it.validateQueueEntry() &&
                            it.queuedForType() == QueueType.Ranked

                        audience.sendActionBar(
                            "${CC.PRI}${it.queuedForType().name} ${it.queuedForTeamSize()}v${it.queuedForTeamSize()} ${
                                it.queuedForKit()?.displayName ?: "???"
                            }${CC.WHITE}${
                                if (shouldIncludeELORange && shouldIncludePingRange)
                                    "" else " ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Queued for ${TimeUtil.formatIntoMMSS((it.queuedForTime() / 1000).toInt())}"
                            }${
                                if (shouldIncludePingRange)
                                {
                                    "${CC.GRAY} ${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Ping: ${CC.BOLD}${
                                        it.queueEntry().leaderRangedPing
                                            .toIntRangeInclusive()
                                            .formattedDomain()
                                    }"
                                } else
                                {
                                    ""
                                }
                            }${
                                if (shouldIncludeELORange)
                                {
                                    "${CC.GRAY} ${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}ELO: ${CC.BOLD}${
                                        it.queueEntry().leaderRangedELO
                                            .toIntRangeInclusive()
                                            .formattedDomain()
                                    }"
                                } else
                                {
                                    ""
                                }
                            }".component
                        )
                    }
            }, 0L, 5L)

        Events
            .subscribe(PlayerSpawnLocationEvent::class.java)
            .handler {
                with(LobbyConfigurationService.cached()) {
                    it.spawnLocation = spawnLocation
                        .toLocation(
                            Bukkit.getWorlds().first()
                        )
                }
            }

        fun flightEffect(player: Player): FlightEffectSetting
        {
            return BasicsProfileService
                .find(player)
                ?.setting(
                    "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:flight-effect",
                    FlightEffectSetting.None
                )
                ?: FlightEffectSetting.None
        }

        val lastParticleTick = mutableMapOf<UUID, Long>()

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .filter {
                it.player.isFlying
            }
            .handler {
                val flightEffect = flightEffect(it.player)
                if (flightEffect == FlightEffectSetting.None)
                {
                    return@handler
                }

                if (lastParticleTick.containsKey(it.player.uniqueId))
                {
                    if (System.currentTimeMillis() - lastParticleTick[it.player.uniqueId]!! < 100L)
                    {
                        return@handler
                    }
                }

                val packet = flightEffect.createPacket()
                val nmsPacket = packet.createPacket(it.player.location)

                Players.all()
                    .filter { other -> other.world == it.player.world }
                    .filter { other -> other.canSee(it.player) }
                    .forEach { player ->
                        if (flightEffect.multiplied)
                        {
                            ReflectionUtils.sendPacket(player, nmsPacket)
                            ReflectionUtils.sendPacket(player, nmsPacket)
                        } else
                        {
                            ReflectionUtils.sendPacket(player, nmsPacket)
                        }
                    }

                lastParticleTick[it.player.uniqueId] = System.currentTimeMillis()
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                val player = LobbyPlayer(event.player.uniqueId)
                playerCache[event.player.uniqueId] = player

                if (event.player.hasPermission("practice.spawn-flight"))
                {
                    player.player.allowFlight = true
                    player.player.isFlying = true
                }

                CompletableFuture.runAsync(player::syncQueueState)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                val profile = playerCache
                    .remove(event.player.uniqueId)
                    ?: return@handler

                if (profile.leaveQueueOnLogout && profile.inQueue())
                {
                    QueueService.leaveQueue(event.player)
                }

                if (profile.state == PlayerState.InTournament)
                {
                    TournamentCommand.onLeave(
                        ScalaPlayer(event.player, audiences, plugin)
                    )
                }
            }
            .bindWith(plugin)

        fun AwareMessage.usePlayer(use: Player.() -> Unit)
        {
            val players = retrieve<List<String>>("playerIDs")
            players.forEach {
                val bukkit = Bukkit
                    .getPlayer(
                        UUID.fromString(it)
                    )
                    ?: return

                use(bukkit)
            }
        }

        aware.listen("send-message") {
            val message = retrieve<String>("message").split("\n")

            usePlayer {
                for (component in message)
                {
                    sendMessage(
                        Color.translate(
                            component
                                .replace("{primary}", CC.PRI)
                                .replace("{secondary}", CC.SEC)
                        )
                    )
                }
            }
        }

        aware.listen("send-action-message") {
            val message = Serializers.gson.fromJson(
                retrieve<String>("message"),
                Message::class.java
            )

            message.components.onEach {
                it.value = it.value
                    .replace("{primary}", CC.PRI)
                    .replace("{secondary}", CC.SEC)
            }

            usePlayer {
                message.sendToPlayer(player)
            }
        }

        aware.listen("send-action-broadcast") {
            val message = Serializers.gson.fromJson(
                retrieve<String>("message"),
                Message::class.java
            )

            message.components.onEach {
                it.value = it.value
                    .replace("{primary}", CC.PRI)
                    .replace("{secondary}", CC.SEC)
            }

            Players.all().forEach(message::sendToPlayer)
        }

        aware.listen("send-notification-sound") {
            val setting = retrieve<String>("setting")

            usePlayer {
                val profile = BasicsProfileService.find(this)
                    ?: return@usePlayer

                if (profile.setting(setting, StateSettingValue.DISABLED) == StateSettingValue.ENABLED)
                {
                    playSound(
                        location,
                        Sound.NOTE_PLING,
                        1.0f,
                        1.0f
                    )
                }
            }
        }

        aware.listen("redirect") {
            usePlayer {
                VelocityRedirectSystem.redirect(
                    this, retrieve("server")
                )
            }
        }
        aware.connect().toCompletableFuture().join()
    }

    fun find(uniqueId: UUID) = playerCache[uniqueId]
    fun find(player: Player) = playerCache[player.uniqueId]
}
