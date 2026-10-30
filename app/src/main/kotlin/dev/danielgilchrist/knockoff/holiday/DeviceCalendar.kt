package dev.danielgilchrist.knockoff.holiday

import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId

data class DeviceCalendar(
    val id: CalendarId,
    val name: String,
    val account: String,
    val kind: CalendarKind,
)
