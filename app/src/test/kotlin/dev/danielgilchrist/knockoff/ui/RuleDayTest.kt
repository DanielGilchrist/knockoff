package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RuleDayTest {
    private val friday = LocalDate.parse("2026-10-30")
    private val work =
        Settings.DEFAULT.rules.rules
            .single()
    private val evening = LocalDateTime.parse("2026-10-29T20:00").atZone(ZoneId.of("Europe/London"))
    private val sydney = ZoneChoice.Fixed(ZoneId.of("Australia/Sydney"))

    @Test
    fun `today is the date in the rule's zone`() {
        assertEquals(
            RuleDay(LocalDate.parse("2026-10-29"), DayOff.Scheduled),
            RuleDay.of(work, evening, ZoneChoice.Device, Holidays.NONE),
        )
        assertEquals(RuleDay(friday, DayOff.Scheduled), RuleDay.of(work, evening, sydney, Holidays.NONE))
        assertEquals(
            RuleDay(friday, DayOff.Scheduled),
            RuleDay.of(work.copy(zone = RuleZone.Chosen(sydney)), evening, ZoneChoice.Device, Holidays.NONE),
        )
    }

    @Test
    fun `today is off when the rule takes that date off`() {
        val onLeave = work.copy(daysOff = DaysOff(listOf(Leave.day(friday)), PublicHolidays.AsUsual))

        assertEquals(DayOff.OnLeave, RuleDay.of(onLeave, evening, sydney, Holidays.NONE).dayOff)
    }
}
