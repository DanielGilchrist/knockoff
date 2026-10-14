package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.ALL_BUT_SUNDAY
import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.londonWithOffset
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.ZoneOffset

class ScheduleTest {
    private val fridayNights = schedule(window(days(FRIDAY), "22:00", "06:00"))

    @Test
    fun `parsing no windows is empty`() {
        Schedule.parse(emptyList()) shouldBe ScheduleParse.Empty
    }

    @ParameterizedTest(name = "{0} is inside work hours until {1}")
    @CsvSource(
        "2026-10-14T08:00, 2026-10-14T17:00",
        "2026-10-14T12:00, 2026-10-14T17:00",
        "2026-10-14T16:59:59.999999999, 2026-10-14T17:00",
        "2026-10-12T08:00, 2026-10-12T17:00",
        "2026-10-16T16:00, 2026-10-16T17:00",
    )
    fun `inside work hours`(
        now: String,
        endsAt: String,
    ) {
        WORK_HOURS.coverageAt(london(now)) shouldBe Coverage.Inside(london(endsAt))
    }

    @ParameterizedTest(name = "{0} is outside work hours until {1}")
    @CsvSource(
        "2026-10-14T07:59:59.999999999, 2026-10-14T08:00",
        "2026-10-14T00:00, 2026-10-14T08:00",
        "2026-10-14T17:00, 2026-10-15T08:00",
        "2026-10-14T22:00, 2026-10-15T08:00",
        "2026-10-16T17:00, 2026-10-19T08:00",
        "2026-10-16T23:30, 2026-10-19T08:00",
        "2026-10-17T12:00, 2026-10-19T08:00",
        "2026-10-18T23:59:59.999999999, 2026-10-19T08:00",
        "2026-10-19T00:00, 2026-10-19T08:00",
    )
    fun `outside work hours`(
        now: String,
        startsAt: String,
    ) {
        WORK_HOURS.coverageAt(london(now)) shouldBe Coverage.Outside(london(startsAt))
    }

    @Test
    fun `friday evening across the autumn change waits for monday in winter time`() {
        WORK_HOURS.coverageAt(london("2026-10-23T18:00")) shouldBe Coverage.Outside(london("2026-10-26T08:00"))
    }

    @Test
    fun `friday evening across the spring change waits for monday in summer time`() {
        WORK_HOURS.coverageAt(london("2026-03-27T18:00")) shouldBe Coverage.Outside(london("2026-03-30T08:00"))
    }

    @ParameterizedTest(name = "{0} is inside friday overnight hours until {1}")
    @CsvSource(
        "2026-10-16T22:00, 2026-10-17T06:00",
        "2026-10-17T03:00, 2026-10-17T06:00",
    )
    fun `inside overnight hours that started on friday`(
        now: String,
        endsAt: String,
    ) {
        fridayNights.coverageAt(london(now)) shouldBe Coverage.Inside(london(endsAt))
    }

    @ParameterizedTest(name = "{0} is outside friday overnight hours until {1}")
    @CsvSource(
        "2026-10-16T21:59, 2026-10-16T22:00",
        "2026-10-17T06:00, 2026-10-23T22:00",
        "2026-10-16T03:00, 2026-10-16T22:00",
        "2026-10-15T23:00, 2026-10-16T22:00",
    )
    fun `outside overnight hours that start on friday`(
        now: String,
        startsAt: String,
    ) {
        fridayNights.coverageAt(london(now)) shouldBe Coverage.Outside(london(startsAt))
    }

    @Test
    fun `consecutive full days merge into one stretch`() {
        val weekend = schedule(window(days(SATURDAY, SUNDAY), "00:00", "00:00"))

        weekend.coverageAt(london("2026-10-17T10:00")) shouldBe Coverage.Inside(london("2026-10-19T00:00"))
        weekend.coverageAt(london("2026-10-16T23:00")) shouldBe Coverage.Outside(london("2026-10-17T00:00"))
    }

    @Test
    fun `adjacent windows merge`() {
        val split = schedule(window(EVERY_DAY, "08:00", "12:00"), window(EVERY_DAY, "12:00", "17:00"))

        split.coverageAt(london("2026-10-14T10:00")) shouldBe Coverage.Inside(london("2026-10-14T17:00"))
    }

    @Test
    fun `overlapping windows merge regardless of order`() {
        val overlapping = schedule(window(EVERY_DAY, "12:00", "17:00"), window(EVERY_DAY, "08:00", "13:00"))

        overlapping.coverageAt(london("2026-10-14T10:00")) shouldBe Coverage.Inside(london("2026-10-14T17:00"))
    }

    @Test
    fun `a window contained in another does not shorten it`() {
        val nested = schedule(window(EVERY_DAY, "08:00", "17:00"), window(EVERY_DAY, "10:00", "11:00"))

        nested.coverageAt(london("2026-10-14T10:30")) shouldBe Coverage.Inside(london("2026-10-14T17:00"))
    }

    @Test
    fun `a gap between windows is outside until the next window`() {
        val gapped = schedule(window(EVERY_DAY, "08:00", "12:00"), window(EVERY_DAY, "13:00", "17:00"))

        gapped.coverageAt(london("2026-10-14T12:30")) shouldBe Coverage.Outside(london("2026-10-14T13:00"))
    }

    @Test
    fun `parsed windows do not follow later changes to the given list`() {
        val given = mutableListOf(window(EVERY_DAY, "08:00", "09:00"))
        val parse = Schedule.parse(given).shouldBeInstanceOf<ScheduleParse.Valid>()

        given.clear()

        parse.schedule.windows shouldBe listOf(window(EVERY_DAY, "08:00", "09:00"))
    }

    @Test
    fun `sunday overnight hours wrapping into monday complete the week`() {
        val wrapped =
            schedule(
                window(days(SUNDAY), "00:00", "12:00"),
                window(days(SUNDAY), "12:00", "08:00"),
                window(days(MONDAY), "08:00", "00:00"),
                window(days(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), "00:00", "00:00"),
            )

        wrapped.coverageAt(london("2026-10-14T12:00")) shouldBe Coverage.Always
    }

    @Test
    fun `sunday overnight hours wrapping into monday stop where they end`() {
        val wrappedThenGap =
            schedule(
                window(days(SUNDAY), "00:00", "12:00"),
                window(days(SUNDAY), "12:00", "08:00"),
                window(days(MONDAY), "09:00", "00:00"),
                window(days(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), "00:00", "00:00"),
            )

        wrappedThenGap.coverageAt(london("2026-10-18T12:00")) shouldBe Coverage.Inside(london("2026-10-19T08:00"))
    }

    @Test
    fun `a half-second gap in the week is not always covered`() {
        val halfSecondApart = schedule(window(EVERY_DAY, "00:00", "12:00"), window(EVERY_DAY, "12:00:00.5", "00:00"))

        halfSecondApart.coverageAt(london("2026-10-14T11:00")) shouldBe Coverage.Inside(london("2026-10-14T12:00"))
    }

    @Test
    fun `a one-second gap in the week is not always covered`() {
        val almost =
            schedule(
                window(ALL_BUT_SUNDAY, "00:00", "00:00"),
                window(days(SUNDAY), "00:00", "23:59:59"),
            )

        almost.coverageAt(london("2026-10-14T12:00")) shouldBe Coverage.Inside(london("2026-10-18T23:59:59"))
    }

    @Test
    fun `every day in full is always covered`() {
        schedule(window(EVERY_DAY, "00:00", "00:00")).coverageAt(london("2026-10-14T12:00")) shouldBe Coverage.Always
    }

    @Test
    fun `every day in full from a non-midnight start is always covered`() {
        schedule(window(EVERY_DAY, "09:00", "09:00")).coverageAt(london("2026-10-14T08:00")) shouldBe Coverage.Always
    }

    @Test
    fun `six full days and a short seventh is not always covered`() {
        val allButThursdayEvening =
            schedule(
                window(days(*DayOfWeek.entries.filter { it != THURSDAY }.toTypedArray()), "00:00", "00:00"),
                window(days(THURSDAY), "00:00", "18:00"),
            )

        allButThursdayEvening.coverageAt(london("2026-10-14T12:00")) shouldBe
            Coverage.Inside(london("2026-10-15T18:00"))
    }

    @Test
    fun `a nightly one-minute gap ends each stretch`() {
        val nearlyAlways = schedule(window(EVERY_DAY, "00:00", "23:59"))

        nearlyAlways.coverageAt(london("2026-10-14T00:00")) shouldBe Coverage.Inside(london("2026-10-14T23:59"))
    }

    @Test
    fun `next start is found a week ahead`() {
        val wednesdays = schedule(window(days(WEDNESDAY), "08:00", "09:00"))

        wednesdays.coverageAt(london("2026-10-14T09:00")) shouldBe Coverage.Outside(london("2026-10-21T08:00"))
    }

    @Test
    fun `hours wholly inside the spring gap wait for the following week`() {
        val skipped = schedule(window(days(SUNDAY), "01:15", "01:45"))

        skipped.coverageAt(london("2026-03-29T00:00")) shouldBe Coverage.Outside(london("2026-04-05T01:15"))
        skipped.coverageAt(london("2026-03-28T12:00")) shouldBe Coverage.Outside(london("2026-04-05T01:15"))
    }

    @Test
    fun `a gap closed by the spring change is not always covered`() {
        val gapOnlyInWinter =
            schedule(
                window(ALL_BUT_SUNDAY, "00:00", "00:00"),
                window(days(SUNDAY), "00:00", "01:00"),
                window(days(SUNDAY), "01:30", "00:00"),
            )

        gapOnlyInWinter.coverageAt(london("2026-03-28T12:00")) shouldBe Coverage.Inside(london("2026-04-05T01:00"))
    }

    @Test
    fun `windows overlapping only in the autumn repeated hour are not always covered`() {
        val overlapOnlyInAutumn =
            schedule(
                window(ALL_BUT_SUNDAY, "00:00", "00:00"),
                window(days(SUNDAY), "00:00", "01:30"),
                window(days(SUNDAY), "01:45", "00:00"),
            )

        overlapOnlyInAutumn.coverageAt(london("2026-10-24T12:00")) shouldBe Coverage.Inside(london("2026-11-01T01:30"))
    }

    @Test
    fun `hours starting in the spring gap cover the wall clock after the jump`() {
        val early = schedule(window(EVERY_DAY, "01:30", "09:00"))

        early.coverageAt(london("2026-03-29T00:30")) shouldBe Coverage.Outside(london("2026-03-29T02:00"))
        early.coverageAt(london("2026-03-29T02:15")) shouldBe Coverage.Inside(london("2026-03-29T09:00"))
    }

    @Test
    fun `the repeated hour in autumn is covered on both passes`() {
        val repeated = schedule(window(EVERY_DAY, "01:30", "02:30"))
        val inside = Coverage.Inside(london("2026-10-25T02:30"))

        repeated.coverageAt(londonWithOffset("2026-10-25T01:45", ZoneOffset.ofHours(1))) shouldBe inside
        repeated.coverageAt(londonWithOffset("2026-10-25T01:45", ZoneOffset.UTC)) shouldBe inside
    }

    @Test
    fun `hours ending in the autumn overlap run until the second pass`() {
        val lateNight = schedule(window(EVERY_DAY, "00:00", "01:30"))
        val secondPass = Coverage.Inside(londonWithOffset("2026-10-25T01:30", ZoneOffset.UTC))

        lateNight.coverageAt(londonWithOffset("2026-10-25T01:15", ZoneOffset.ofHours(1))) shouldBe secondPass
        lateNight.coverageAt(londonWithOffset("2026-10-25T01:15", ZoneOffset.UTC)) shouldBe secondPass
    }
}
