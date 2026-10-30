package dev.danielgilchrist.knockoff.holiday

import dev.danielgilchrist.knockoff.domain.dayoff.HolidayEvent

sealed interface HolidayRead {
    data class Read(
        val calendar: DeviceCalendar,
        val events: List<HolidayEvent>,
    ) : HolidayRead

    data object NoPermission : HolidayRead

    data object Missing : HolidayRead

    data class Failed(
        val detail: String,
    ) : HolidayRead
}
