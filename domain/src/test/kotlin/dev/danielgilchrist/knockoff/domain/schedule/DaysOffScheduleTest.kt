package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.breakNamed
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

class DaysOffScheduleTest {
    private val wednesday = setOf(LocalDate.parse("2026-10-14"))
    private val always = schedule(window(EVERY_DAY, "00:00", "00:00"))

    @Test
    fun `a day off skips that day's hours until the next working day`() {
        WORK_HOURS.coverageAt(london("2026-10-14T07:00"), wednesday) shouldBe
            Coverage.Outside(london("2026-10-15T08:00"))
        WORK_HOURS.coverageAt(london("2026-10-14T12:00"), wednesday) shouldBe
            Coverage.Outside(london("2026-10-15T08:00"))
    }

    @Test
    fun `the day before a day off runs as usual`() {
        WORK_HOURS.coverageAt(london("2026-10-13T12:00"), wednesday) shouldBe
            Coverage.Inside(london("2026-10-13T17:00"))
        WORK_HOURS.coverageAt(london("2026-10-13T20:00"), wednesday) shouldBe
            Coverage.Outside(london("2026-10-15T08:00"))
    }

    @Test
    fun `a past day off changes nothing`() {
        WORK_HOURS.coverageAt(london("2026-10-15T12:00"), wednesday) shouldBe
            Coverage.Inside(london("2026-10-15T17:00"))
        always.coverageAt(london("2026-10-15T12:00"), wednesday) shouldBe Coverage.Always
    }

    @Test
    fun `hours covering the whole week end at the next day off`() {
        always.coverageAt(london("2026-10-12T12:00"), wednesday) shouldBe Coverage.Inside(london("2026-10-14T00:00"))
    }

    @Test
    fun `during a day off from hours covering the whole week they resume at midnight`() {
        always.coverageAt(london("2026-10-14T12:00"), wednesday) shouldBe Coverage.Outside(london("2026-10-15T00:00"))
    }

    @Test
    fun `a day off months ahead of hours covering the whole week is found`() {
        always.coverageAt(london("2026-10-14T12:00"), setOf(LocalDate.parse("2027-03-01"))) shouldBe
            Coverage.Inside(london("2027-03-01T00:00"))
    }

    @Test
    fun `leave longer than a fortnight waits for the hours after it`() {
        val leave = (0L..34L).map { LocalDate.parse("2026-10-12").plusDays(it) }.toSet()

        WORK_HOURS.coverageAt(london("2026-10-14T12:00"), leave) shouldBe Coverage.Outside(london("2026-11-16T08:00"))
    }

    @Test
    fun `weekly hours with every occurrence off for months wait for the next one`() {
        val mondays = schedule(window(days(MONDAY), "09:00", "10:00"))
        val off = (0L..10L).map { LocalDate.parse("2026-10-19").plusWeeks(it) }.toSet()

        mondays.coverageAt(london("2026-10-14T12:00"), off) shouldBe Coverage.Outside(london("2027-01-04T09:00"))
    }

    @Test
    fun `a day off cuts overnight hours at midnight on both sides`() {
        val fridayNights = schedule(window(days(FRIDAY), "22:00", "06:00"))
        val friday = setOf(LocalDate.parse("2026-10-16"))
        val saturday = setOf(LocalDate.parse("2026-10-17"))

        fridayNights.coverageAt(london("2026-10-16T21:00"), friday) shouldBe
            Coverage.Outside(london("2026-10-17T00:00"))
        fridayNights.coverageAt(london("2026-10-17T01:00"), friday) shouldBe Coverage.Inside(london("2026-10-17T06:00"))
        fridayNights.coverageAt(london("2026-10-16T23:00"), saturday) shouldBe
            Coverage.Inside(london("2026-10-17T00:00"))
    }

    @Test
    fun `a day off and a break together leave the next working day's hours split`() {
        val lunch =
            schedule(
                window(EVERY_DAY, "08:00", "17:00"),
                breaks = listOf(breakNamed("Lunch", window(EVERY_DAY, "12:00", "13:00"))),
            )

        lunch.coverageAt(london("2026-10-14T09:00"), wednesday) shouldBe Coverage.Outside(london("2026-10-15T08:00"))
        lunch.coverageAt(london("2026-10-15T09:00"), wednesday) shouldBe Coverage.Inside(london("2026-10-15T12:00"))
    }

    @Test
    fun `a day off on the autumn change lasts all 25 hours`() {
        val sunday = setOf(LocalDate.parse("2026-10-25"))

        always.coverageAt(london("2026-10-25T12:00"), sunday) shouldBe Coverage.Outside(london("2026-10-26T00:00"))
        always.coverageAt(london("2026-10-24T12:00"), sunday) shouldBe Coverage.Inside(london("2026-10-25T00:00"))
    }

    @Test
    fun `a day off on the spring change lasts its 23 hours`() {
        val sunday = setOf(LocalDate.parse("2026-03-29"))

        always.coverageAt(london("2026-03-29T12:00"), sunday) shouldBe Coverage.Outside(london("2026-03-30T00:00"))
    }
}
