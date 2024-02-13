package gg.tropic.practice.games

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.basics.plugin.shutdown.ServerShutdownEvent
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.staff.anticheat.AnticheatCheck
import gg.scala.staff.anticheat.AnticheatFeature
import gg.tropic.game.extensions.cosmetics.CosmeticLocalConfig
import gg.tropic.game.extensions.cosmetics.CosmeticRegistry
import gg.tropic.game.extensions.cosmetics.killeffects.KillEffectCosmeticCategory
import gg.tropic.game.extensions.cosmetics.killeffects.cosmetics.KillEffect
import gg.tropic.game.extensions.cosmetics.killeffects.cosmetics.LightningKillEffect
import gg.tropic.game.extensions.cosmetics.messagebundles.KillMessageBundleCosmeticCategory
import gg.tropic.game.extensions.cosmetics.messagebundles.cosmetics.MessageBundle
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.services.GameManagerService
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.suffixWhenDev
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants.HEART_SYMBOL
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Bukkit
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
import kotlin.math.ceil
import kotlin.math.min

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

    private val communicationLayer by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:communications".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(plugin.logger)
            .build()
    }

    val gameMappings = mutableMapOf<UUID, GameImpl>()

    val playerToGameMappings = Object2ReferenceOpenHashMap<UUID, GameImpl>(Bukkit.getMaxPlayers(), 0.75F)
    val spectatorToGameMappings = Object2ReferenceOpenHashMap<UUID, GameImpl>(Bukkit.getMaxPlayers(), 0.75F)

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
        CosmeticLocalConfig.enableCosmeticResources = false

        Events
            .subscribe(ServerShutdownEvent::class.java)
            .handler {
                gameMappings.values.forEach { game ->
                    game.complete(null, "Server rebooting")
                }
            }

        AnticheatFeature.configureAlertFilter {

            /**
             * (game.expectationModel.queueType == null ||
             *                 game.expectationModel.queueType == QueueType.Casual) &&
             *                 game.expectationModel.queueId != "tournament"
             */
            return@configureAlertFilter it.type == AnticheatCheck.DOUBLE_CLICK || it.type == AnticheatCheck.AUTO_CLICKER
        }

        communicationLayer.listen("terminate") {
            val matchID = retrieve<UUID>("matchID")
            val terminator = retrieveNullable<UUID>("terminator")
            val reason = retrieveNullable<String>("reason")
                ?: "Ended by an administrator"
            val game = gameMappings[matchID]
                ?: return@listen

            game.complete(null, reason = reason)

            QuickAccess.sendGlobalBroadcast(
                "${CC.L_PURPLE}[P] ${CC.D_PURPLE}[${
                    ServerSync.getLocalGameServer().id
                }] ${CC.L_PURPLE}Match ${CC.WHITE}${
                    matchID.toString().substring(0..5)
                }${CC.L_PURPLE} was terminated by ${CC.B_WHITE}${
                    terminator?.username() ?: "Console"
                }${CC.L_PURPLE} for ${CC.WHITE}${
                    reason
                }${CC.L_PURPLE}.",
                permission = "practice.admin"
            )
        }
        communicationLayer.connect()

        GameManagerService.bindToStatusService {
            GameStatus(
                games = gameMappings.values
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
                            state = it.state,
                            players = it.toPlayers(),
                            spectators = it.arenaWorld?.players
                                ?.filter { player ->
                                    player.hasMetadata("spectator")
                                }
                                ?.map(Player::getUniqueId)
                                ?: listOf(),
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

        fun killer(event: PlayerDeathEvent?): Entity?
        {
            val entityDamageEvent = event
                ?.entity?.lastDamageCause

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

        fun runDeathEffectsFor(
            player: Player, target: Player, game: GameImpl, gameIsFinished: Boolean
        )
        {
            val killerCosmetic = CosmeticRegistry
                .getAllEquipped(
                    KillEffectCosmeticCategory,
                    player
                )
                .randomOrNull()
                ?: LightningKillEffect

            val killEffectCosmetic = killerCosmetic as KillEffect
            killEffectCosmetic.applyTo(
                game.toBukkitPlayers().filterNotNull(),
                player, target
            )

            val configuration = killEffectCosmetic.serveConfiguration(player)
            if (configuration.flight != false && gameIsFinished)
            {
                player.allowFlight = true
                player.isFlying = true
            }

            target.allowFlight = true
            target.isFlying = true

            if (configuration.clearInventory != false && gameIsFinished)
            {
                game.takeSnapshot(player)

                player.inventory.clear()
                player.updateInventory()
            }
        }

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

            return "${CC.GOLD}${bundle.random()}${CC.GRAY}"
        }

        data class GameRemovalEvent(
            val drops: MutableList<ItemStack>,
            val shouldRespawn: Boolean = true,
            val deathEvent: PlayerDeathEvent? = null
        )

        fun Player.gracefullyRemoveFromGame(event: GameRemovalEvent)
        {
            val game = byPlayer(this)
                ?: return kotlin.run {
                    event.drops.clear()
                }

            game.takeSnapshot(this)

            health = maxHealth
            foodLevel = 20
            saturation = 20.0F

            if (event.shouldRespawn)
            {
                spigot().respawn()
            }

            setMetadata(
                "spectator", FixedMetadataValue(plugin, true)
            )

            event.drops.removeIf { stack ->
                stack.type == Material.POTION || stack.type == Material.BOWL
            }

            val team = game.getTeamOf(this)
            val noAlive = if (team.players.size > 1)
                team.nonSpectators().isEmpty() else true

            if (!noAlive)
            {
                game.toBukkitPlayers()
                    .filterNotNull()
                    .forEach { other ->
                        VisibilityHandler.update(other)
                        NametagHandler.reloadPlayer(this, other)
                    }
            }

            val killerPlayer = killer(event.deathEvent)
            val killer = if (killerPlayer !is Player || killerPlayer == this)
            {
                game.getOpponent(this)
            } else
            {
                game.getTeamOf(killerPlayer)
            }

            if (killerPlayer is Player)
            {
                runDeathEffectsFor(killerPlayer, this, game, noAlive)
            }

            game.sendMessage(
                "${CC.RED}$name${CC.GRAY} was ${
                    if (killerPlayer == null || killerPlayer == this) "killed" else "${
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

            with(PracticeProfileService.find(this)) {
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

        Events
            .subscribe(PlayerItemConsumeEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (!game.state(GameState.Playing))
                {
                    return@handler
                }

                if (
                    !it.player.isDead &&
                    it.player.itemInHand.type == Material.MUSHROOM_SOUP &&
                    it.player.health < 19.0
                )
                {
                    val newHealth = min(it.player.health + 7.0, 20.0)

                    it.player.health = newHealth
                    it.player.itemInHand.type = Material.BOWL
                    it.player.updateInventory()
                }
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

        Events
            .subscribe(PlayerItemConsumeEvent::class.java)
            .filter {
                it.item.type == Material.POTION
            }
            .handler {
                Tasks.sync {
                    if (it.player.itemInHand.type != Material.GLASS_BOTTLE)
                    {
                        return@sync
                    }

                    it.player.itemInHand = ItemStack(Material.AIR)
                    it.player.updateInventory()
                }
            }
            .bindWith(plugin)

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
                    if (
                        game.flag(FeatureFlag.DeathOnLiquidInteraction) &&
                        it.to.block.isLiquid
                    )
                    {
                        it.player.gracefullyRemoveFromGame(event = GameRemovalEvent(
                            drops = mutableListOf(),
                            shouldRespawn = false
                        ))
                        return@handler
                    }

                    val yAxis = game
                        .flagMetaData(
                            FeatureFlag.DeathBelowYAxis, "level"
                        )
                        ?.toIntOrNull()

                    if (yAxis != null && it.to.y < yAxis)
                    {
                        it.player.gracefullyRemoveFromGame(event = GameRemovalEvent(
                            drops = mutableListOf(),
                            shouldRespawn = false
                        ))
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

        Events.subscribe(PlayerDeathEvent::class.java)
            .handler {
                it.entity.gracefullyRemoveFromGame(event = GameRemovalEvent(
                    drops = it.drops,
                    shouldRespawn = true,
                    deathEvent = it
                ))
            }
            .bindWith(plugin)

        Events
            .subscribe(PotionSplashEvent::class.java)
            .handler {
                it.affectedEntities.removeIf { entity ->
                    entity.hasMetadata("spectator")
                }

                val shooter = it.entity.shooter
                if (shooter is Player)
                {
                    val game = byPlayer(shooter)
                        ?: return@handler run {
                            it.isCancelled = true
                        }

                    val counter = game.counter(shooter)
                    val intensity = it.getIntensity(shooter)
                    val effect = it.potion.effects
                        .firstOrNull { effect -> effect.type == PotionEffectType.HEAL }
                        ?: return@handler

                    counter.increment("totalPots")
                    counter.increment(if (intensity <= 0.5) "missedPots" else "hitPots")

                    val amountHealed = (intensity * (4 shl effect.amplifier) + 0.5)
                    if (shooter.health + amountHealed > shooter.maxHealth)
                    {
                        counter.increment(
                            "wastedHeals",
                            (shooter.health + amountHealed) - shooter.maxHealth
                        )
                    }
                }
            }

        Events
            .subscribe(EntityRegainHealthEvent::class.java)
            .filter { it.entity is Player }
            .handler {
                val game = byPlayer(it.entity as Player)
                    ?: return@handler

                if (it.regainReason == EntityRegainHealthEvent.RegainReason.REGEN)
                {
                    game.counter(it.entity as Player)
                        .apply {
                            increment("healthRegained", it.amount)
                        }
                }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                val spectator = bySpectator(it.player.uniqueId)
                    ?: return@handler

                spectator.expectedSpectators -= it.player.uniqueId
            }

        Schedulers
            .async()
            .runRepeating({ _ ->
                gameMappings.values.forEach {
                    if (it.state != GameState.Playing)
                    {
                        return@runRepeating
                    }

                    if (it.toBukkitPlayers().all { player -> player == null })
                    {
                        Tasks.sync {
                            kotlin.runCatching {
                                it.complete(null, reason = "Zero players alive in match")
                            }.onFailure(Throwable::printStackTrace)
                        }
                    }
                }
            }, 0L, 20L)

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
                        game.state = GameState.Completed
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

        Events
            .subscribe(PlayerBlockedHitEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                game.counter(it.player)
                    .apply {
                        // TODO: configurable
                        if (valueOf("blockedHits") >= 10)
                        {
                            it.isCancelled = true
                            return@handler
                        }

                        increment("blockedHits")
                    }
            }

        configureMetadataProvidingEventHandler<PlayerCriticalHitEvent>("criticalHits")

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter {
                it.entity is Player
            }
            .handler {
                val game = byPlayerOrSpectator(it.entity.uniqueId)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

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

                game
                    .counter(it.entity as Player)
                    .apply {
                        reset("combo")
                    }

                game
                    .counter(it.damager as Player)
                    .apply {
                        increment("totalHits")
                        increment("combo")

                        if (valueOf("combo") > valueOf("highestCombo"))
                        {
                            update("highestCombo", valueOf("combo"))
                        }
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
                    // TODO: multiplayer support teams
                    runDeathEffectsFor(
                        it.damager as Player,
                        it.entity as Player,
                        game, true
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
        playerToGameMappings[player.uniqueId]

    fun byPlayer(player: UUID) =
        playerToGameMappings[player]

    fun bySpectator(player: UUID) =
        spectatorToGameMappings[player]

    fun byPlayerOrSpectator(player: UUID) =
        playerToGameMappings[player]
            ?: spectatorToGameMappings[player]

    fun iterativeByPlayerOrSpectator(player: UUID) = gameMappings.values
        .find {
            player in it.toPlayers() || player in it.expectedSpectators
        }

    private inline fun <reified T : PlayerEvent> configureMetadataProvidingEventHandler(incrementing: String)
    {
        Events
            .subscribe(T::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                game.counter(it.player)
                    .apply {
                        increment(incrementing)
                    }
            }
    }
}
