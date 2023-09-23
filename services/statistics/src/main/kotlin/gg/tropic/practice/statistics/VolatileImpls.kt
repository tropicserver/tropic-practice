package gg.tropic.practice.statistics

import org.joda.time.DateTime

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class SingleDayLifetime<T : Any>(
    defaultValue: T
) : Volatile<T>(defaultValue)
{
    override fun lifetime(): DateTime.() -> DateTime = {
        plusDays(1)
    }
}

class SingleWeekLifetime<T : Any>(
    defaultValue: T
) : Volatile<T>(defaultValue)
{
    override fun lifetime(): DateTime.() -> DateTime = {
        plusWeeks(1)
    }
}
