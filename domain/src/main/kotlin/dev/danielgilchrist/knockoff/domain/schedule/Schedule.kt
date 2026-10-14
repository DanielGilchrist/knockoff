package dev.danielgilchrist.knockoff.domain.schedule

import java.time.LocalDate
import java.time.ZonedDateTime

@ConsistentCopyVisibility
data class Schedule private constructor(
    val windows: List<Window>,
) {
    companion object {
        private const val DAYS_AHEAD = 14L

        internal fun of(window: Window): Schedule = Schedule(listOf(window))

        fun parse(windows: List<Window>): ScheduleParse =
            when {
                windows.isEmpty() -> ScheduleParse.Empty
                else -> ScheduleParse.Valid(Schedule(windows.toList()))
            }
    }

    private val coversWholeWeek: Boolean =
        windows
            .flatMap { it.spansOfStandardWeek() }
            .sortedBy { it.startNano }
            .fold(0L) { reached, span -> if (span.startNano <= reached) maxOf(reached, span.endNano) else reached }
            .let { it >= WeekSpan.NANOS_PER_WEEK }

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
