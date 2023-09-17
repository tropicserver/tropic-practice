package gg.tropic.practice.kit.feature

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
enum class FeatureFlag(
    val schema: MutableMap<String, String> = mutableMapOf(),
    val incompatibleWith: () -> Set<FeatureFlag> = { emptySet() },
    val requires: Set<FeatureFlag> = setOf()
)
{
    Ranked,
    HeartsBelowNameTag,
    DoNotTakeHealth,
    DoNotTakeHunger,
    DoNotRemoveArmor,
    RequiresBuildMap,
    PlaceBlocks,
    BreakAllBlocks(
        incompatibleWith = { setOf(BreakPlacedBlocks) },
        requires = setOf(RequiresBuildMap)
    ),
    BreakPlacedBlocks(
        incompatibleWith = { setOf(BreakAllBlocks) },
        requires = setOf(RequiresBuildMap)
    ),
    MaxBuildHeight(
        requires = setOf(RequiresBuildMap, PlaceBlocks)
    ),
    FrozenOnGameStart,
    NewlyCreated,
    MenuOrderWeight(
        schema = mutableMapOf("weight" to "0")
    ),
    ExpireBlocksAfterSeconds(
        schema = mutableMapOf("time" to "10"),
        requires = setOf(PlaceBlocks)
    ),
    PlayersDoNotTakeDamage,
    PlayersDoNotLoseHealthOnDamage(
        incompatibleWith = { setOf(PlayersDoNotTakeDamage) }
    ),
    DeathOnLiquidInteraction,
    // multi-round
    MultiRound,
    ImmediateRespawnOnDeath(
        incompatibleWith = { setOf(StartNewRoundOnDeath) }
    ),
    StartNewRoundOnDeath(
        incompatibleWith = { setOf(ImmediateRespawnOnDeath) }
    ),
    RoundsRequiredToCompleteGame(
        schema = mutableMapOf("value" to "2")
    ),
    TimeUserSpectatesAfterDeath(
        schema = mutableMapOf("value" to "3")
    ),
    CountDownTimeBeforeRoundStart(
        schema = mutableMapOf("value" to "5")
    ),
    FrozenOnRoundStart(
        // game start freeze is implied with round start
        incompatibleWith = { setOf(FrozenOnGameStart) }
    ),
    StartNewRoundOnPortalEnter(
        incompatibleWith = { setOf(StartNewRoundOnDeath) }
    ),
    RemovePlacedBlocksOnRoundStart
}
