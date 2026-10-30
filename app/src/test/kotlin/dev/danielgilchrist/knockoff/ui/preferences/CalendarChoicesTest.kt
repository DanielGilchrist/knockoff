package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.holiday.CalendarKind
import dev.danielgilchrist.knockoff.holiday.CalendarList
import dev.danielgilchrist.knockoff.holiday.DeviceCalendar
import dev.danielgilchrist.knockoff.ui.PermissionAsk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarChoicesTest {
    @Test
    fun `listed calendars are sorted by name whatever their case`() {
        val uk =
            DeviceCalendar(CalendarId(3), "Holidays in the United Kingdom", "me@example.com", CalendarKind.Holidays)
        val christian = DeviceCalendar(CalendarId(2), "christian Holidays", "me@example.com", CalendarKind.Holidays)

        assertEquals(
            CalendarChoices.Listed(listOf(christian, uk)),
            CalendarChoices.of(CalendarList.Listed(listOf(uk, christian)), PermissionAsk.Askable),
        )
    }

    @Test
    fun `without permission it asks until asking is blocked`() {
        assertEquals(CalendarChoices.NotAllowed, CalendarChoices.of(CalendarList.NoPermission, PermissionAsk.Askable))
        assertEquals(CalendarChoices.Blocked, CalendarChoices.of(CalendarList.NoPermission, PermissionAsk.Blocked))
        assertEquals(
            CalendarChoices.Failed("boom"),
            CalendarChoices.of(CalendarList.Failed("boom"), PermissionAsk.Askable),
        )
    }
}
