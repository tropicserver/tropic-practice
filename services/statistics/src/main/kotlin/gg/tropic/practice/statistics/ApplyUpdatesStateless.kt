package gg.tropic.practice.statistics

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class ApplyUpdatesStateless(
    private val update: List<() -> Unit>,
    private val inherits: ApplyUpdatesStateless? = null
)
{
    fun update()
    {
        update.forEach { it() }
        inherits?.update()
    }
}
