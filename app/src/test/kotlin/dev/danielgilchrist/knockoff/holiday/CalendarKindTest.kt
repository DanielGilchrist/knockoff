package dev.danielgilchrist.knockoff.holiday

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarKindTest {
    @Test
    fun `google's holiday calendars are told apart by their owner`() {
        assertEquals(CalendarKind.Holidays, CalendarKind.of("en-gb.uk#holiday@group.v.calendar.google.com"))
        assertEquals(CalendarKind.Other, CalendarKind.of("someone@example.com"))
        assertEquals(CalendarKind.Other, CalendarKind.of(""))
    }
}
