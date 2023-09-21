package gg.tropic.practice.kit.statistics

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class Volatile<T : Any>(
    private val defaultValue: T,
    private val lifetime: Long
)
{
    private var value = defaultValue
    private var lastValidation =
        System.currentTimeMillis()

    operator fun divAssign(value: T)
    {
        this.value = value
    }

    operator fun invoke(): T
    {
        return value
    }

    fun get(): T
    {
        if (System.currentTimeMillis() - lastValidation > lifetime)
        {
            value = defaultValue
            lastValidation = System.currentTimeMillis()
        }

        return value
    }
}
