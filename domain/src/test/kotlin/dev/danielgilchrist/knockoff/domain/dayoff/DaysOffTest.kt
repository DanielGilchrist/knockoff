package dev.danielgilchrist.knockoff.domain.dayoff

import dev.danielgilchrist.knockoff.domain.leave
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class DaysOffTest {
    private val today = date("2026-10-30")
    private val past = leave("2026-10-01", "2026-10-02")
    private val sick = Leave.day(today)
    private val christmas = leave("2026-12-24", "2026-12-28")
    private val holidays = Holidays(setOf(date("2026-12-25"), date("2027-01-01")))

    @ParameterizedTest(name = "{0} with public holidays {1} is {2}")
    @CsvSource(
        "2026-10-30, Off, OnLeave",
        "2026-10-30, AsUsual, OnLeave",
        "2026-12-25, Off, OnLeave",
        "2027-01-01, Off, PublicHoliday",
        "2027-01-01, AsUsual, Scheduled",
        "2026-10-31, Off, Scheduled",
    )
    fun `a date is off for leave first, then public holidays`(
        date: String,
        publicHolidays: PublicHolidays,
        expected: DayOff,
    ) {
        DaysOff(listOf(sick, christmas), publicHolidays).offOn(date(date), holidays) shouldBe expected
    }

    @Test
    fun `the dates off include public holidays only when they're off`() {
        DaysOff(listOf(sick), PublicHolidays.Off).dates(holidays) shouldBe setOf(today) + holidays.dates
        DaysOff(listOf(sick), PublicHolidays.AsUsual).dates(holidays) shouldBe setOf(today)
    }

    @Test
    fun `upcoming leave is what hasn't ended, soonest first`() {
        DaysOff(listOf(christmas, past, sick), PublicHolidays.AsUsual).upcoming(today) shouldBe listOf(sick, christmas)
    }

    @Test
    fun `taking leave adds it once and forgets leave that has ended`() {
        DaysOff(listOf(christmas, past), PublicHolidays.Off).taking(sick, today) shouldBe
            DaysOff(listOf(sick, christmas), PublicHolidays.Off)
        DaysOff(listOf(sick), PublicHolidays.Off).taking(sick, today) shouldBe DaysOff(listOf(sick), PublicHolidays.Off)
    }

    @Test
    fun `cancelling leave removes only it`() {
        DaysOff(listOf(sick, christmas), PublicHolidays.Off).cancelling(christmas) shouldBe
            DaysOff(listOf(sick), PublicHolidays.Off)
    }

    @Test
    fun `back on a date ends every leave covering it there`() {
        val overlapping = leave("2026-10-29", "2026-11-02")

        DaysOff(listOf(sick, overlapping, christmas), PublicHolidays.Off).backOn(today) shouldBe
            DaysOff(
                listOf(leave("2026-10-29", "2026-10-29"), leave("2026-10-31", "2026-11-02"), christmas),
                PublicHolidays.Off,
            )
    }

    private fun date(text: String): LocalDate = LocalDate.parse(text)
}
