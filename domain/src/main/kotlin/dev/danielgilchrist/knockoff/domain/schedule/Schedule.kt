package dev.danielgilchrist.knockoff.domain.schedule

import java.time.LocalDate
import java.time.ZonedDateTime

@ConsistentCopyVisibility
data class Schedule private constructor(
    val windows: List<Window>,
    val breaks: List<Break>,
) {
    companion object {
        private const val DAYS_AHEAD = 14L

        internal fun of(window: Window): Schedule = Schedule(listOf(window), emptyList())

        fun parse(
            windows: List<Window>,
            breaks: List<Break>,
        ): ScheduleParse =
            when {
                windows.isEmpty() -> ScheduleParse.Empty
                coveredNanos(windows, breaks) == 0L -> ScheduleParse.NothingLeftAfterBreaks
                else -> ScheduleParse.Valid(Schedule(windows.toList(), breaks.toList()))
            }

        private fun coveredNanos(
            windows: List<Window>,
            breaks: List<Break>,
        ): Long =
            windows
                .flatMap { it.spansOfStandardWeek() }
                .merged()
                .without(breaks.flatMap { it.window.spansOfStandardWeek() })
                .nanosWithinWeek()
    }

    private val coversWholeWeek: Boolean = coveredNanos(windows, breaks) == WeekSpan.NANOS_PER_WEEK

    fun coverageAt(now: ZonedDateTime): Coverage {
        if (coversWholeWeek) return Coverage.Always

        val instant = now.toInstant()
        val today = now.toLocalDate()
        val dates = (-1L..DAYS_AHEAD).map { today.plusDays(it) }
        val endOfLastDate = today.plusDays(DAYS_AHEAD + 1).atStartOfDay(now.zone).toInstant()
        val current = stretches(dates, now).first { instant < it.end }

        check(current.end < endOfLastDate) { "Coverage at $now runs past the generated dates: $current" }

        return when {
            instant in current -> Coverage.Inside(current.end.atZone(now.zone))
            else -> Coverage.Outside(current.start.atZone(now.zone))
        }
    }

    private fun stretches(
        dates: List<LocalDate>,
        now: ZonedDateTime,
    ): List<Interval> {
        val cuts = breaks.flatMap { it.window.occurrencesStartingWithin(dates, now.zone) }

        return merged(dates, now).flatMap { stretch ->
            cuts.fold(listOf(stretch)) { pieces, cut -> pieces.flatMap { it.without(cut) } }
        }
    }

    private fun merged(
        dates: List<LocalDate>,
        now: ZonedDateTime,
    ): List<Interval> =
        windows
            .flatMap { it.occurrencesStartingWithin(dates, now.zone) }
            .sortedBy { it.start }
            .fold(emptyList()) { merged, next ->
                val last = merged.lastOrNull()

                when {
                    last != null && last.touches(next) -> merged.dropLast(1) + last.span(next)
                    else -> merged + next
                }
            }
}
