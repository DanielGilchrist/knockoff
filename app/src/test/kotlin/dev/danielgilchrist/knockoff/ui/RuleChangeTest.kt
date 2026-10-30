package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RuleChangeTest {
    private val today = LocalDate.parse("2026-10-30")
    private val work =
        Settings.DEFAULT.rules.rules
            .single()

    @Test
    fun `taking today off adds it, and back today removes it`() {
        val off = RuleChange.TakeOff(today).applyTo(work)

        assertEquals(DaysOff(listOf(Leave.day(today)), PublicHolidays.AsUsual), off.daysOff)
        assertEquals(DaysOff.NONE, RuleChange.BackOn(today).applyTo(off).daysOff)
    }
}
