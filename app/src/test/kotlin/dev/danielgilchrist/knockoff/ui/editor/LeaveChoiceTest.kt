package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.leave
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LeaveChoiceTest {
    private val christmas = LocalDate.parse("2026-12-25")
    private val boxingDay = LocalDate.parse("2026-12-26")

    @Test
    fun `a start alone is one day, a start and end a range, and nothing is unpicked`() {
        assertEquals(LeaveChoice.Unpicked, LeaveChoice.of(null, null))
        assertEquals(LeaveChoice.Unpicked, LeaveChoice.of(null, boxingDay))
        assertEquals(LeaveChoice.Picked(Leave.day(christmas)), LeaveChoice.of(christmas, null))
        assertEquals(
            LeaveChoice.Picked(leave("2026-12-25", "2026-12-26")),
            LeaveChoice.of(christmas, boxingDay),
        )
        assertEquals(LeaveChoice.Unpicked, LeaveChoice.of(boxingDay, christmas))
    }
}
