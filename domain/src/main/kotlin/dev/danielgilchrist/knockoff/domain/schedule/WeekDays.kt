package dev.danielgilchrist.knockoff.domain.schedule

import java.time.DayOfWeek

@ConsistentCopyVisibility
data class WeekDays private constructor(
    val days: Set<DayOfWeek>,
) {
    companion object {
        internal val WORKING_WEEK =
            WeekDays(
                setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            )

        fun parse(days: Set<DayOfWeek>): WeekDaysParse =
            when {
                days.isEmpty() -> WeekDaysParse.Empty
                else -> WeekDaysParse.Valid(WeekDays(days.toSet()))
            }
    }

    operator fun contains(day: DayOfWeek): Boolean = day in days
}
