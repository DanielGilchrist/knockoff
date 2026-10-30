package dev.danielgilchrist.knockoff.holiday

sealed interface CalendarList {
    data class Listed(
        val calendars: List<DeviceCalendar>,
    ) : CalendarList

    data object NoPermission : CalendarList

    data class Failed(
        val detail: String,
    ) : CalendarList
}
