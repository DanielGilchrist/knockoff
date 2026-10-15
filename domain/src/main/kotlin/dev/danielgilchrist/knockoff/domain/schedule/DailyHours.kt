package dev.danielgilchrist.knockoff.domain.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed class DailyHours {
    companion object {
        fun between(
            start: LocalTime,
            end: LocalTime,
        ): DailyHours =
            when {
                start < end -> SameDay(start, end)
                end < start -> Overnight(start, end)
                else -> FullDay(start)
            }
    }

    abstract val start: LocalTime
    abstract val end: LocalTime

    internal val lengthInNanos: Long
        get() =
            when (this) {
                is SameDay -> end.toNanoOfDay() - start.toNanoOfDay()
                is Overnight -> WeekSpan.NANOS_PER_DAY - start.toNanoOfDay() + end.toNanoOfDay()
                is FullDay -> WeekSpan.NANOS_PER_DAY
            }

    @ConsistentCopyVisibility
    data class SameDay internal constructor(
        override val start: LocalTime,
        override val end: LocalTime,
    ) : DailyHours()

    @ConsistentCopyVisibility
    data class Overnight internal constructor(
        override val start: LocalTime,
        override val end: LocalTime,
    ) : DailyHours()

    @ConsistentCopyVisibility
    data class FullDay internal constructor(
        override val start: LocalTime,
    ) : DailyHours() {
        override val end: LocalTime get() = start
    }

    internal fun occurrenceStartingOn(
        date: LocalDate,
        zone: ZoneId,
    ): Occurrence {
        val endDate =
            when (this) {
                is SameDay -> date
                is Overnight, is FullDay -> date.plusDays(1)
            }
        val first = date.atTime(start).firstInstantIn(zone)
        val last = endDate.atTime(end).lastInstantIn(zone)

        return when (val parse = Interval.parse(first, last)) {
            is IntervalParse.Valid -> Occurrence.Occurs(parse.interval)
            IntervalParse.Empty -> Occurrence.SkippedByClockChange
        }
    }
}
