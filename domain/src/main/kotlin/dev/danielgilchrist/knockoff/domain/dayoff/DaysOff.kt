package dev.danielgilchrist.knockoff.domain.dayoff

import java.time.LocalDate

data class DaysOff(
    val leave: List<Leave>,
    val publicHolidays: PublicHolidays,
) {
    companion object {
        val NONE: DaysOff get() = DaysOff(emptyList(), PublicHolidays.AsUsual)
    }

    fun offOn(
        date: LocalDate,
        holidays: Holidays,
    ): DayOff =
        when {
            leave.any { date in it } -> DayOff.OnLeave
            date in dates(holidays) -> DayOff.PublicHoliday
            else -> DayOff.Scheduled
        }

    fun upcoming(today: LocalDate): List<Leave> = leave.filterNot { it.endsBefore(today) }.sortedBy { it.first }

    fun taking(
        added: Leave,
        today: LocalDate,
    ): DaysOff = copy(leave = (upcoming(today) + added).distinct().sortedBy { it.first })

    fun cancelling(removed: Leave): DaysOff = copy(leave = leave - removed)

    fun backOn(date: LocalDate): DaysOff = copy(leave = leave.flatMap { it.without(date) })

    internal fun dates(holidays: Holidays): Set<LocalDate> {
        val leaveDates = leave.flatMap { it.dates }.toSet()

        return when (publicHolidays) {
            PublicHolidays.Off -> leaveDates + holidays.dates
            PublicHolidays.AsUsual -> leaveDates
        }
    }
}
