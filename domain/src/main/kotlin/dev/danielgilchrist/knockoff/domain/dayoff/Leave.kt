package dev.danielgilchrist.knockoff.domain.dayoff

import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ConsistentCopyVisibility
data class Leave private constructor(
    val first: LocalDate,
    val last: LocalDate,
) {
    companion object {
        fun day(date: LocalDate): Leave = Leave(date, date)

        fun parse(
            first: LocalDate,
            last: LocalDate,
        ): LeaveParse =
            when {
                last < first -> LeaveParse.EndsBeforeStart
                else -> LeaveParse.Valid(Leave(first, last))
            }
    }

    val dates: List<LocalDate>
        get() = (0L..ChronoUnit.DAYS.between(first, last)).map { first.plusDays(it) }

    operator fun contains(date: LocalDate): Boolean = date in first..last

    internal fun endsBefore(date: LocalDate): Boolean = last < date

    internal fun without(date: LocalDate): List<Leave> =
        when (date) {
            !in this -> {
                listOf(this)
            }

            else -> {
                listOf(parse(first, date.minusDays(1)), parse(date.plusDays(1), last)).flatMap { part ->
                    when (part) {
                        is LeaveParse.Valid -> listOf(part.leave)
                        LeaveParse.EndsBeforeStart -> emptyList()
                    }
                }
            }
        }
}
