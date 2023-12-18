package gg.tropic.practice.games

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.tropic.game.extensions.cosmetics.CosmeticRegistry
import gg.tropic.game.extensions.cosmetics.killeffects.KillEffectCosmeticCategory
import gg.tropic.game.extensions.cosmetics.killeffects.cosmetics.KillEffect
import gg.tropic.game.extensions.cosmetics.messagebundles.KillMessageBundleCosmeticCategory
import gg.tropic.game.extensions.cosmetics.messagebundles.cosmetics.MessageBundle
import gg.tropic.game.extensions.profile.CorePlayerProfileService
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.services.GameManagerService
import gg.tropic.practice.games.models.GameReference
import gg.tropic.practice.games.models.GameStatus
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.statistics.KitStatistics
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.cooldown.Cooldown
import me.lucko.helper.cooldown.CooldownMap
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants.HEART_SYMBOL
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Service
object GameService
{
    @Inject
    lateinit var plugin: PracticeGame

    @Inject
    lateinit var redirector: ServerAggregateHandler

    val games = mutableMapOf<UUID, GameImpl>()

    private val ensureCauseDenied = listOf(
        DamageCause.BLOCK_EXPLOSION,
        DamageCause.FIRE,
        DamageCause.LAVA,
        DamageCause.STARVATION,
        DamageCause.LIGHTNING
    )

    @Configure
    fun configure()
    {
        GameManagerService.bindToStatusService {
            GameStatus(
                games = games.values
                    .map {
                        val players = it.toBukkitPlayers()
                            .filterNotNull()

                        val majoritySpectatorsEnabled = players
                            .mapNotNull(BasicsProfileService::find)
                            .count { profile ->
                                profile.setting(
                                    "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:allow-spectators",
                                    StateSettingValue.ENABLED
                                ) == StateSettingValue.ENABLED
                            }

                        GameReference(
                            uniqueId = it.identifier,
                            mapID = it.map.name,
                            state = it.state.toString(),
                            players = it.toPlayers(),
                            spectators = it.arenaWorld.players
                                .filter { player ->
                                    player.hasMetadata("spectator")
                                }
                                .map(Player::getUniqueId),
                            kitID = it.kit.id,
                            replicationID = it.arenaWorldName!!,
                            server = ServerSync.local.id,
                            queueId = it.expectationModel.queueId,
                            majorityAllowsSpectators = players.isEmpty() ||
                                (majoritySpectatorsEnabled / players.size) >= 0.50
                        )
                    }
            )
        }

        fun overridePotionEffect(
            player: Player, effect: PotionEffect
        )
        {
            if (player.hasPotionEffect(effect.type))
            {
                player.removePotionEffect(effect.type)
            }

            player.addPotionEffect(effect)
        }

        Events.subscribe(ProjectileLaunchEvent::class.java)
            .handler {
                val fishingHook = it.entity

                if (it.entityType == EntityType.FISHING_HOOK)
                {
                    fishingHook.velocity = fishingHook
                        .velocity.multiply(1.17)
                }
            }
            .bindWith(plugin)

        Events.subscribe(CraftItemEvent::class.java)
            .handler {
                it.isCancelled = true
            }
            .bindWith(plugin)

        Events.subscribe(PlayerItemConsumeEvent::class.java)
            .filter {
                it.item.hasItemMeta() && it.item.itemMeta.displayName.contains("Golden Head")
            }
            .handler {
                it.isCancelled = true
                it.player.playSound(
                    it.player.location, Sound.EAT, 10f, 1f
                )

                it.player.removePotionEffect(PotionEffectType.ABSORPTION)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.ABSORPTION,
                        120 * 20, 0, false, true
                    )
                )

                it.player.removePotionEffect(PotionEffectType.REGENERATION)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.REGENERATION,
                        5 * 20, 2, false, true
                    )
                )

                it.player.removePotionEffect(PotionEffectType.SPEED)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SPEED,
                        10 * 20, 0, false, true
                    )
                )

                if (it.item.amount == 1)
                {
                    it.player.itemInHand = ItemStack(Material.AIR)
                } else
                {
                    it.item.amount -= 1
                }
            }
            .bindWith(plugin)

        Events.subscribe(PlayerItemConsumeEvent::class.java)
            .filter { it.item.type == Material.GOLDEN_APPLE }
            .handler {
                overridePotionEffect(
                    it.player, PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 2)
                )

                overridePotionEffect(
                    it.player, PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0)
                )
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerDropItemEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler kotlin.run {
                        it.isCancelled = true
                    }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.itemDrop.itemStack.type.name.contains("SWORD"))
                {
                    it.isCancelled = true
                    it.player.sendMessage("${CC.RED}You cannot drop your sword!")
                }

                if (it.itemDrop.itemStack.type == Material.GLASS_BOTTLE)
                {
                    Schedulers.sync().runLater(it.itemDrop::remove, 1L)
                }
            }
            .bindWith(plugin)

        Events.subscribe(PlayerPickupItemEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler kotlin.run {
                        it.isCancelled = true
                    }

                if (
                    !game.ensurePlaying() ||
                    it.player.hasMetadata("spectator")
                )
                {
                    it.isCancelled = true
                }
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (game.ensurePlaying())
                {
                    fun completeGameArbitraryKiller()
                    {
                        game.sendMessage(
                            "${CC.RED}${it.player.name}${CC.GRAY} was killed!"
                        )

                        game.complete(
                            game.getOpponent(it.player)
                        )
                    }

                    if (
                        game.flag(FeatureFlag.DeathOnLiquidInteraction) &&
                        it.to.block.isLiquid
                    )
                    {
                        completeGameArbitraryKiller()
                        return@handler
                    }

                    val yAxis = game
                        .flagMetaData(
                            FeatureFlag.DeathBelowYAxis, "level"
                        )
                        ?.toIntOrNull()

                    if (yAxis != null && it.to.y < yAxis)
                    {
                        completeGameArbitraryKiller()
                        return@handler
                    }

                    return@handler
                }

                if (
                    game.state(GameState.Starting) &&
                    game.flag(FeatureFlag.FrozenOnGameStart)
                )
                {
                    it.player.teleport(it.from)
                }
            }
            .bindWith(plugin)

        fun killer(event: PlayerDeathEvent): Entity?
        {
            val entityDamageEvent = event
                .entity.lastDamageCause

            if (
                entityDamageEvent != null &&
                !entityDamageEvent.isCancelled &&
                entityDamageEvent is EntityDamageByEntityEvent
            )
            {
                val damager = entityDamageEvent.damager

                if (damager is Projectile)
                {
                    val shooter = damager.shooter

                    if (shooter != null)
                    {
                        return shooter as Entity
                    }
                }

                return damager
            }

            return null
        }

        Events.subscribe(FoodLevelChangeEvent::class.java)
            .handler {
                val game = byPlayer(it.entity as Player)
                    ?: return@handler

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (game.flag(FeatureFlag.DoNotTakeHunger))
                {
                    it.isCancelled = true
                }
            }
            .bindWith(plugin)

        fun getMessageBundlePhrase(player: Player): String
        {
            val bundle = CosmeticRegistry
                .findRelatedTo(KillMessageBundleCosmeticCategory)
                .filter { like ->
                    like.equipped(player)
                }
                .filterIsInstance<MessageBundle>()
                .flatMap { bundle -> bundle.phrases }

            if (bundle.isEmpty())
            {
                return "slain"
            }

            return "${CC.B_WHITE}${bundle.random()}${CC.GRAY}"
        }

        fun runDeathEffectsFor(player: Player, target: Player, game: GameImpl)
        {
            val killerCosmetic = CosmeticRegistry
                .getSingleEquipped(
                    KillEffectCosmeticCategory,
                    player
                )

            if (killerCosmetic != null)
            {
                (killerCosmetic as KillEffect).applyTo(
                    game.toBukkitPlayers().filterNotNull(),
                    target
                )
            }
        }

        Events.subscribe(PlayerDeathEvent::class.java)
            .handler {
                val game = byPlayer(it.entity)
                    ?: return@handler kotlin.run {
                        it.drops.clear()
                    }

                val team = game.getTeamOf(it.entity)
                game.takeSnapshot(it.entity)

                it.deathMessage = null

                it.entity.health = it.entity.maxHealth
                it.entity.foodLevel = 20
                it.entity.saturation = 20.0F

                it.entity.spigot().respawn()
                it.entity.setMetadata(
                    "spectator", FixedMetadataValue(plugin, true)
                )

                it.drops.removeIf {
                    it.type == Material.POTION || it.type == Material.BOWL
                }

                val noAlive = if (team.players.size > 1)
                    team.nonSpectators().isEmpty() else true

                if (!noAlive)
                {
                    game.toBukkitPlayers()
                        .filterNotNull()
                        .forEach { other ->
                            VisibilityHandler.update(other)
                            NametagHandler.reloadPlayer(it.entity, other)
                        }
                }

                val killerPlayer = killer(it)
                val killer = if (killerPlayer !is Player)
                {
                    game.getOpponent(it.entity)
                } else
                {
                    game.getTeamOf(killerPlayer)
                }

                if (killerPlayer is Player)
                {
                    runDeathEffectsFor(killerPlayer, it.entity, game)
                }

                game.sendMessage(
                    "${CC.RED}${it.entity.name}${CC.GRAY} was ${
                        if (killerPlayer == null) "killed" else "${
                            getMessageBundlePhrase(killerPlayer as Player)
                        } by ${CC.GREEN}${killerPlayer.name}${CC.GRAY}"
                    }!"
                )

                if (killerPlayer is Player)
                {
                    with(PracticeProfileService.find(killerPlayer)) {
                        if (this != null)
                        {
                            useKitStatistics(game) {
                                kills += 1
                            }

                            globalStatistics.userKilledOpponent().apply()
                            save()
                        }
                    }
                }

                with(PracticeProfileService.find(it.entity)) {
                    if (this != null)
                    {
                        useKitStatistics(game) {
                            deaths += 1
                        }

                        globalStatistics.userWasKilled().apply()
                        save()
                    }
                }

                if (noAlive)
                {
                    game.complete(killer)
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                val spectator = bySpectator(it.player.uniqueId)
                    ?: return@handler

                spectator.expectedSpectators -= it.player.uniqueId
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (game.ensurePlaying())
                {
                    val team = game.getTeamOf(it.player)

                    it.player.setMetadata(
                        "spectator",
                        FixedMetadataValue(plugin, true)
                    )

                    val noAlive = if (team.players.size > 1)
                        team.nonSpectators().isEmpty() else true

                    game.takeSnapshot(it.player)

                    game.sendMessage(
                        "${CC.RED}${it.player.name}${CC.GRAY} left the game."
                    )

                    if (noAlive)
                    {
                        game.complete(
                            game.getOpponent(it.player)
                        )
                    }
                } else
                {
                    if (
                        game.state(GameState.Starting) ||
                        game.state(GameState.Waiting)
                    )
                    {
                        game.closeAndCleanup()
                    }
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter { it.entity is Player }
            .handler {
                if (it.entity.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (ensureCauseDenied.contains(it.cause))
                {
                    it.isCancelled = true
                    return@handler
                }

                val game = byPlayer(
                    it.entity as Player
                )
                    ?: return@handler

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }
            }
            .bindWith(plugin)

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is FishHook && ((it.damager as FishHook).shooter) is Player && it.entity is Player }
            .handler { event ->
                val game = byPlayer(
                    event.entity as Player
                )
                    ?: return@handler

                val damagerGame = byPlayer(
                    ((event.damager as FishHook).shooter) as Player
                )
                    ?: return@handler

                if (damagerGame.expectation == game.expectation)
                {
                    val damagerTeam = game
                        .getTeamOf(((event.damager as FishHook).shooter) as Player)

                    val team = game
                        .getTeamOf(event.entity as Player)

                    if (damagerTeam.side == team.side)
                    {
                        event.isCancelled = true
                        return@handler
                    }
                }
            }

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Arrow && ((it.damager as Arrow).shooter) is Player && it.entity is Player }
            .handler { event ->
                val game = byPlayer(
                    event.entity as Player
                )
                    ?: return@handler

                val damagerGame = byPlayer(
                    ((event.damager as Arrow).shooter) as Player
                )
                    ?: return@handler

                if (damagerGame.expectation == game.expectation)
                {
                    val damagerTeam = game
                        .getTeamOf(((event.damager as Arrow).shooter) as Player)

                    val team = game
                        .getTeamOf(event.entity as Player)

                    if (damagerTeam.side == team.side)
                    {
                        event.isCancelled = true
                        return@handler
                    }
                }

                val entity = event.entity as Player
                val arrow = event.damager as Arrow

                if (arrow.shooter is Player)
                {
                    val shooter = arrow.shooter as Player

                    if (entity.name != shooter.name && event.damage != 0.0)
                    {
                        val health = ceil(entity.health - event.finalDamage) / 2.0

                        if (health > 0.0)
                        {
                            shooter.sendMessage("${entity.displayName}${CC.GREEN} is now at ${CC.RED}$health${HEART_SYMBOL}${CC.GREEN}!")
                        }
                    }
                }
            }

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter {
                it.entity is Player
            }
            .handler {
                val game = byPlayer(
                    it.entity as Player
                )
                    ?: return@handler

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (
                    it.damager.hasMetadata("spectator") ||
                    it.entity.hasMetadata("spectator")
                )
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.damager !is Player)
                {
                    if (!game.ensurePlaying())
                    {
                        it.isCancelled = true
                    }

                    return@handler
                }

                val damagerGame = byPlayer(
                    it.damager as Player
                )
                    ?: return@handler

                val damagerTeam = game.getTeamOf(it.damager as Player)
                val team = game.getTeamOf(it.entity as Player)

                if (damagerGame.expectation == game.expectation)
                {
                    if (damagerTeam.side == team.side)
                    {
                        it.isCancelled = true
                        it.damager.sendMessage(
                            "${CC.RED}You cannot damage your teammates!"
                        )
                        return@handler
                    }
                }

                val doNotTakeDamage = game
                    .flagMetaData(
                        FeatureFlag.DoNotTakeDamage, "doDamageTick"
                    )
                    ?.toBooleanStrictOrNull()

                damagerTeam.combinedHits += 1
                damagerTeam.playerCombos.compute(
                    it.damager!!.uniqueId
                ) { _, previous ->
                    val computed = (previous ?: 0) + 1
                    val highestCombo = damagerTeam
                        .highestPlayerCombos[it.damager.uniqueId]

                    if (highestCombo == null || highestCombo < computed)
                    {
                        damagerTeam.highestPlayerCombos[it.damager.uniqueId] = computed
                    }

                    computed
                }

                team.playerCombos.remove(it.entity.uniqueId)

                val winWhenHitsReached = game
                    .flagMetaData(
                        FeatureFlag.WinWhenNHitsReached, "hits"
                    )
                    ?.toIntOrNull()

                if (
                    winWhenHitsReached != null &&
                    damagerTeam.combinedHits >= winWhenHitsReached
                )
                {
                    runDeathEffectsFor(
                        it.damager as Player,
                        it.entity as Player,
                        game
                    )
                    game.complete(damagerTeam)
                }

                if (doNotTakeDamage != null)
                {
                    if (doNotTakeDamage)
                    {
                        it.damage = 0.0
                        return@handler
                    }

                    it.isCancelled = true
                }
            }

        Events.subscribe(BlockBreakEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (it.player.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (game.flag(FeatureFlag.BreakAllBlocks))
                {
                    return@handler
                }

                if (
                    game.flag(FeatureFlag.BreakPlacedBlocks) &&
                    it.block.hasMetadata("placed")
                )
                {
                    return@handler
                }

                if (game.kit.allowedBlockTypeMappings.isPresent)
                {
                    if (
                        game.kit.allowedBlockTypeMappings.get()
                            .any { (type, data) ->
                                it.block.type == type && it.block.data == data.toByte()
                            }
                    )
                    {
                        return@handler
                    }
                }

                it.isCancelled = true
            }
            .bindWith(plugin)

        val validBlockPlace = listOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
        )

        Events.subscribe(BlockPlaceEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (it.player.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.flag(FeatureFlag.PlaceBlocks))
                {
                    it.isCancelled = true
                    return@handler
                }

                val zone = game.map.findZoneContainingEntity(it.player)

                if (zone != null && zone.id.startsWith("restrict"))
                {
                    it.isCancelled = true
                    it.player.sendMessage(
                        "${CC.RED}You cannot build in this area!"
                    )
                    return@handler
                }

                val levelRestrictions = game.map.findMapLevelRestrictions()

                if (levelRestrictions != null)
                {
                    if (it.block.y !in levelRestrictions.range)
                    {
                        if (
                            !levelRestrictions.allowBuildOnBlockSideBlockFaces ||
                            validBlockPlace
                                .any { face ->
                                    it.blockPlaced.getRelative(face)
                                        .hasMetadata("placed")
                                }
                        )
                        {
                            it.isCancelled = true
                            it.player.sendMessage(
                                "${CC.RED}You cannot build in this area!"
                            )
                        }
                        return@handler
                    }
                }

                // TODO: add the vector to a list in GameImpl so we can reset if needed for multi round.
                it.blockPlaced.setMetadata(
                    "placed",
                    FixedMetadataValue(plugin, true)
                )

                val blockExpiration = game
                    .flagMetaData(
                        FeatureFlag.ExpirePlacedBlocksAfterNSeconds,
                        "time"
                    )
                    ?.toIntOrNull()

                if (blockExpiration != null)
                {
                    Schedulers
                        .sync()
                        .runLater({
                            if (it.blockPlaced.hasMetadata("placed"))
                            {
                                it.blockPlaced.type = Material.AIR
                            }
                        }, blockExpiration * 20L)
                }
            }
            .bindWith(plugin)
    }

    fun byPlayer(player: Player) =
        games.values
            .find {
                player.uniqueId in it.toPlayers()
            }

    fun byPlayer(player: UUID) =
        games.values
            .find {
                player in it.toPlayers()
            }

    fun bySpectator(player: UUID) =
        games.values
            .find {
                player in it.expectedSpectators
            }

    fun byPlayerOrSpectator(player: UUID) =
        games.values
            .find {
                player in it.toPlayers() || player in it.expectedSpectators
            }
}
