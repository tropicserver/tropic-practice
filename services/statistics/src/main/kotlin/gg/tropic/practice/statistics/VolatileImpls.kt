package gg.tropic.practice.statistics

import org.joda.time.DateTime

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
class SingleDayVolatile<T : Any>(
    defaultValue: T,
    override val lifetime: DateTime.() -> DateTime = {
        plusDays(1)
    }
) : Volatile<T>(defaultValue)

class SingleWeekVolatile<T : Any>(
    defaultValue: T,
    override val lifetime: DateTime.() -> DateTime = {
        plusWeeks(1)
    }
) : Volatile<T>(defaultValue)
