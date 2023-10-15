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
    QueueSizes(
        schema = mutableMapOf("sizes" to "1")
    ),
    HeartsBelowNameTag,
    DoNotTakeDamage(
        schema = mutableMapOf("doDamageTick" to "false")
    ),
    DoNotTakeHunger,
    DoNotRemoveArmor,
    RequiresBuildMap,
    PlaceBlocks,
    BreakAllBlocks(
        incompatibleWith = { setOf(BreakPlacedBlocks, BreakSpecificBlockTypes) },
        requires = setOf(RequiresBuildMap)
    ),
    BreakPlacedBlocks(
        incompatibleWith = { setOf(BreakAllBlocks) },
        requires = setOf(RequiresBuildMap)
    ),
    BreakSpecificBlockTypes(
        incompatibleWith = { setOf(BreakAllBlocks) },
        requires = setOf(RequiresBuildMap),
        schema = mutableMapOf(
            // Default for bridges LMAO
            "types" to "STAINED_CLAY:11,STAINDED_CLAY:14"
        )
    ),
    FrozenOnGameStart,
    NewlyCreated,
    MenuOrderWeight(
        schema = mutableMapOf("weight" to "0")
    ),
    ExpirePlacedBlocksAfterNSeconds(
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
