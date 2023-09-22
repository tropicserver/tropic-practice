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
    fun apply()
    {
        update.forEach { it() }
        inherits?.apply()
    }
}
