package gg.tropic.practice.statistics.leaderboards

import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType

/**
 * @author GrowlyX
 * @since 12/16/2023
 */
fun floorDateTime(lastValidation: Long, lifetime: DateTime.() -> DateTime) = Period(
    DateTime.now(),
    lifetime(
        DateTime(lastValidation)
    ),
    PeriodType.millis()
).millis
