package dev.danielgilchrist.knockoff.domain.dayoff

sealed interface HolidayCalendar {
    data object None : HolidayCalendar

    data class Chosen(
        val calendar: CalendarId,
        val regions: Set<HolidayRegion>,
    ) : HolidayCalendar
}
