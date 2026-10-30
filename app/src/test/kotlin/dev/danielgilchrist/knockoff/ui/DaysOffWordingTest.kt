package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayEvent
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayKind
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.holiday.CalendarKind
import dev.danielgilchrist.knockoff.holiday.DeviceCalendar
import dev.danielgilchrist.knockoff.holiday.HolidayRead
import dev.danielgilchrist.knockoff.holiday.HolidaysState
import dev.danielgilchrist.knockoff.leave
import dev.danielgilchrist.knockoff.region
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DaysOffWordingTest {
    private val today = date("2026-10-30")
    private val uk =
        DeviceCalendar(CalendarId(3), "Holidays in the United Kingdom", "me@example.com", CalendarKind.Holidays)
    private val england = region("England")
    private val chosen = HolidayCalendar.Chosen(uk.id, setOf(region("Wales"), england))

    @Test
    fun `leave reads as today, a date or a range`() {
        assertEquals("Today", Leave.day(today).describe(today))
        assertEquals("31 Oct", Leave.day(date("2026-10-31")).describe(today))
        assertEquals("24–28 Dec", leave("2026-12-24", "2026-12-28").describe(today))
        assertEquals("30 Dec – 2 Jan", leave("2026-12-30", "2027-01-02").describe(today))
        assertEquals("Fri 30 Oct", Leave.day(today).describeSpan())
        assertEquals("5 days", leave("2026-12-24", "2026-12-28").describeSpan())
    }

    @Test
    fun `days off list upcoming leave, at most three, then public holidays`() {
        val past = leave("2026-10-01", "2026-10-02")
        val many =
            listOf(past, Leave.day(today)) + (1L..3L).map { Leave.day(today.plusWeeks(it)) }

        assertEquals("None", DaysOff(listOf(past), PublicHolidays.AsUsual).describe(today))
        assertEquals("Public holidays", DaysOff.NONE.copy(publicHolidays = PublicHolidays.Off).describe(today))
        assertEquals(
            "Today · 6 Nov · 13 Nov · 1 more · Public holidays",
            DaysOff(many, PublicHolidays.Off).describe(today),
        )
    }

    @Test
    fun `a day off replaces the rule's sentence with what it does today`() {
        val overview = RuleOverview("Slack only reaches you on weekdays 08:00–17:00.", "", "Slack", "")

        assertEquals(overview.sentence, summaryOn(DayOff.Scheduled, Policy.DeliverOnlyDuring, overview))
        assertEquals(
            "Day off today. Holding Slack all day.",
            summaryOn(DayOff.OnLeave, Policy.DeliverOnlyDuring, overview),
        )
        assertEquals(
            "Public holiday today. Letting Slack through all day.",
            summaryOn(DayOff.PublicHoliday, Policy.HoldDuring, overview),
        )
    }

    @Test
    fun `the quick action and caption follow the policy`() {
        assertEquals("Take today off", Policy.DeliverOnlyDuring.takeTodayOff())
        assertEquals("Pause for today", Policy.HoldDuring.takeTodayOff())
        assertEquals("Hold Slack until midnight", Policy.DeliverOnlyDuring.takeTodayOffEffect("Slack"))
        assertEquals("Let Slack through until midnight", Policy.HoldDuring.takeTodayOffEffect("Slack"))
        assertEquals(
            "On a day off, this rule holds its apps from midnight to midnight. The next day is as usual.",
            Policy.DeliverOnlyDuring.daysOffCaption(),
        )
        assertEquals(
            "On a day off, this rule lets its apps through from midnight to midnight. The next day is as usual.",
            Policy.HoldDuring.daysOffCaption(),
        )
    }

    @Test
    fun `the holiday source says where holidays come from or what's missing`() {
        assertEquals("Reading the holiday calendar…", HolidaysState.Loading.describeSource())
        assertEquals(
            "No holiday calendar yet. Set one up in Settings › Public holidays.",
            HolidaysState.NotChosen.describeSource(),
        )
        assertEquals(
            "From Holidays in the United Kingdom, with England, Wales",
            HolidaysState.Chosen.of(chosen, HolidayRead.Read(uk, emptyList()), HolidaysState.Loading).describeSource(),
        )
        assertEquals(
            "From Holidays in the United Kingdom",
            HolidaysState.Chosen
                .of(
                    chosen.copy(regions = emptySet()),
                    HolidayRead.Read(uk, emptyList()),
                    HolidaysState.Loading,
                ).describeSource(),
        )
        assertEquals(
            "Knockoff can't read calendars. Allow it in Settings › Public holidays.",
            HolidaysState.Chosen.of(chosen, HolidayRead.NoPermission, HolidaysState.Loading).describeSource(),
        )
        assertEquals(
            "The holiday calendar is no longer on this phone. Choose another in Settings.",
            HolidaysState.Chosen.of(chosen, HolidayRead.Missing, HolidaysState.Loading).describeSource(),
        )
        assertEquals(
            "The holiday calendar couldn't be read.",
            HolidaysState.Chosen.of(chosen, HolidayRead.Failed("boom"), HolidaysState.Loading).describeSource(),
        )
    }

    @Test
    fun `the calendar row names the calendar and regions or what's missing`() {
        assertEquals("Reading…", HolidaysState.Loading.describeCalendar())
        assertEquals("Not set up", HolidaysState.NotChosen.describeCalendar())
        assertEquals(
            "Holidays in the United Kingdom · England · Wales",
            HolidaysState.Chosen
                .of(
                    chosen,
                    HolidayRead.Read(uk, emptyList()),
                    HolidaysState.Loading,
                ).describeCalendar(),
        )
        assertEquals(
            "Needs calendar access",
            HolidaysState.Chosen.of(chosen, HolidayRead.NoPermission, HolidaysState.Loading).describeCalendar(),
        )
        assertEquals(
            "Calendar no longer on this phone",
            HolidaysState.Chosen.of(chosen, HolidayRead.Missing, HolidaysState.Loading).describeCalendar(),
        )
        assertEquals(
            "Couldn't be read",
            HolidaysState.Chosen.of(chosen, HolidayRead.Failed("boom"), HolidaysState.Loading).describeCalendar(),
        )
    }

    @Test
    fun `holidays are known once a chosen calendar is read, and a failed read keeps the same calendar's last`() {
        val christmas = Holidays(setOf(date("2026-12-25")))
        val events = listOf(HolidayEvent(date("2026-12-25"), HolidayKind.Everywhere))
        val read = HolidaysState.Chosen.of(chosen, HolidayRead.Read(uk, events), HolidaysState.Loading)
        val failed = HolidayRead.Failed("boom")
        val other = HolidayCalendar.Chosen(CalendarId(1), emptySet())

        assertEquals(Holidays.NONE, HolidaysState.Loading.holidays)
        assertEquals(Holidays.NONE, HolidaysState.NotChosen.holidays)
        assertEquals(christmas, read.holidays)
        assertEquals(christmas, HolidaysState.Chosen.of(chosen, failed, read).holidays)
        assertEquals(Holidays.NONE, HolidaysState.Chosen.of(other, failed, read).holidays)
        assertEquals(Holidays.NONE, HolidaysState.Chosen.of(chosen, failed, HolidaysState.NotChosen).holidays)
        assertEquals(Holidays.NONE, HolidaysState.Chosen.of(chosen, failed, HolidaysState.Loading).holidays)
        assertEquals(Holidays.NONE, HolidaysState.Chosen.of(chosen, HolidayRead.Missing, read).holidays)
        assertEquals(Holidays.NONE, HolidaysState.Chosen.of(chosen, HolidayRead.NoPermission, read).holidays)
    }

    private fun date(text: String): LocalDate = LocalDate.parse(text)
}
