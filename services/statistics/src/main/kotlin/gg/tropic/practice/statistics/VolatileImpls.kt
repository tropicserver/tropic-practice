package gg.tropic.practice.statistics

import org.joda.time.DateTime

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class SingleDayLifetime<T : Any>(
    defaultValue: T,
    override val lifetime: DateTime.() -> DateTime = {
        plusDays(1)
    }
) : Volatile<T>(defaultValue)

class SingleWeekLifetime<T : Any>(
    defaultValue: T,
    override val lifetime: DateTime.() -> DateTime = {
        plusWeeks(1)
    }
) : Volatile<T>(defaultValue)
