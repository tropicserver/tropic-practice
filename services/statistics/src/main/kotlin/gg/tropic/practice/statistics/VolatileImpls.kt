package gg.tropic.practice.statistics

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
private var nycTimeZone: DateTimeZone = DateTimeZone.forID("America/New_York")

class SingleDayLifetime<T : Any>(
    defaultValue: T
) : Volatile<T>(defaultValue)
{
    override fun lifetime(): DateTime.() -> DateTime = {
        plusDays(1)
            .withZone(nycTimeZone)
            .withTimeAtStartOfDay()
    }
}

class SingleWeekLifetime<T : Any>(
    defaultValue: T
) : Volatile<T>(defaultValue)
{
    override fun lifetime(): DateTime.() -> DateTime = {
        weekOfWeekyear().roundFloorCopy()
            .withZone(nycTimeZone)
            .withTimeAtStartOfDay()
    }
}
