package gg.tropic.practice.kit.statistics

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class ApplyUpdates<T>(
    private val update: List<(T) -> Unit>
)
{
    fun update(value: T)
    {
        update.forEach { it(value) }
    }
}
