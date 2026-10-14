package dev.danielgilchrist.knockoff.domain.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class Window(
    val days: WeekDays,
    val hours: DailyHours,
) {
    companion object {
        val WORKING_HOURS: Window
            get() = Window(WeekDays.WORKING_WEEK, DailyHours.between(LocalTime.of(8, 0), LocalTime.of(17, 0)))
    }

    internal fun occurrencesStartingWithin(
        dates: List<LocalDate>,
        zone: ZoneId,
    ): List<Interval> =
        dates
            .filter { it.dayOfWeek in days }
            .map { hours.occurrenceStartingOn(it, zone) }
            .flatMap { occurrence ->
                when (occurrence) {
                    is Occurrence.Occurs -> listOf(occurrence.interval)
                    Occurrence.SkippedByClockChange -> emptyList()
                }
            }

    internal fun spansOfStandardWeek(): List<WeekSpan> =
        days.days.flatMap { day ->
            val start = day.ordinal * WeekSpan.NANOS_PER_DAY + hours.start.toNanoOfDay()
            val span = WeekSpan(start, start + hours.lengthInNanos)

            listOf(span, span.fromPreviousWeek())
        }
}
