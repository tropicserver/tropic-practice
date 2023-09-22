package gg.tropic.practice.statistics

import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
abstract class Volatile<T : Any>(
    val defaultValue: T
)
{
    abstract val lifetime: DateTime.() -> DateTime

    private var value = defaultValue
    private var lastValidation = DateTime
        .now()
        .millis

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
        if (lifetime(DateTime(lastValidation)).isAfterNow)
        {
            value = defaultValue
            lastValidation = DateTime.now().millis
        }

        return value
    }

    fun timeUntilNextRefreshMillis() =
        Period(
            DateTime.now(),
            lifetime(
                DateTime(lastValidation)
            ),
            PeriodType.millis()
        ).millis
}
