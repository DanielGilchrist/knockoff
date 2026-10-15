package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.LONDON
import dev.danielgilchrist.knockoff.domain.interval
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DailyHoursTest {
    private val eight = LocalTime.of(8, 0)
    private val five = LocalTime.of(17, 0)
    private val ordinaryDay = LocalDate.parse("2026-10-14")
    private val springForward = LocalDate.parse("2026-03-29")
    private val fallBack = LocalDate.parse("2026-10-25")

    @Test
    fun `start before end is same-day`() {
        DailyHours.between(eight, five) shouldBe DailyHours.SameDay(eight, five)
    }

    @Test
    fun `end before start is overnight`() {
        DailyHours.between(five, eight) shouldBe DailyHours.Overnight(five, eight)
    }

    @Test
    fun `equal start and end is a full day`() {
        DailyHours.between(eight, eight) shouldBe DailyHours.FullDay(eight)
    }

    @Test
    fun `same-day occurrence stays on its date`() {
        hours("08:00", "17:00").occurrenceStartingOn(ordinaryDay, LONDON) shouldBe
            occurs("2026-10-14T07:00:00Z", "2026-10-14T16:00:00Z")
    }

    @Test
    fun `overnight occurrence ends on the following date`() {
        hours("17:00", "08:00").occurrenceStartingOn(ordinaryDay, LONDON) shouldBe
            occurs("2026-10-14T16:00:00Z", "2026-10-15T07:00:00Z")
    }

    @Test
    fun `full day occurrence lasts until the same time on the following date`() {
        hours("08:00", "08:00").occurrenceStartingOn(ordinaryDay, LONDON) shouldBe
            occurs("2026-10-14T07:00:00Z", "2026-10-15T07:00:00Z")
    }

    @Test
    fun `full day across the autumn change lasts 25 hours`() {
        hours("00:00", "00:00").occurrenceStartingOn(fallBack, LONDON) shouldBe
            occurs("2026-10-24T23:00:00Z", "2026-10-26T00:00:00Z")
    }

    @Test
    fun `full day across the spring change lasts 23 hours`() {
        hours("00:00", "00:00").occurrenceStartingOn(springForward, LONDON) shouldBe
            occurs("2026-03-29T00:00:00Z", "2026-03-29T23:00:00Z")
    }

    @Test
    fun `hours starting in the spring gap start when the clocks jump`() {
        hours("01:30", "09:00").occurrenceStartingOn(springForward, LONDON) shouldBe
            occurs("2026-03-29T01:00:00Z", "2026-03-29T08:00:00Z")
    }

    @Test
    fun `hours ending in the spring gap end when the clocks jump`() {
        hours("00:30", "01:30").occurrenceStartingOn(springForward, LONDON) shouldBe
            occurs("2026-03-29T00:30:00Z", "2026-03-29T01:00:00Z")
    }

    @Test
    fun `hours straddling the end of the spring gap keep only the part that exists`() {
        hours("01:30", "02:15").occurrenceStartingOn(springForward, LONDON) shouldBe
            occurs("2026-03-29T01:00:00Z", "2026-03-29T01:15:00Z")
    }

    @Test
    fun `hours wholly inside the spring gap are skipped`() {
        hours("01:15", "01:45").occurrenceStartingOn(springForward, LONDON) shouldBe
            Occurrence.SkippedByClockChange
    }

    @Test
    fun `hours that are exactly the spring gap are skipped`() {
        hours("01:00", "02:00").occurrenceStartingOn(springForward, LONDON) shouldBe
            Occurrence.SkippedByClockChange
    }

    @Test
    fun `hours starting in the autumn overlap start on the first pass`() {
        hours("01:30", "02:30").occurrenceStartingOn(fallBack, LONDON) shouldBe
            occurs("2026-10-25T00:30:00Z", "2026-10-25T02:30:00Z")
    }

    @Test
    fun `same-day length is the gap between start and end`() {
        hours("08:00", "17:30").lengthInNanos shouldBe 34_200_000_000_000
    }

    @Test
    fun `overnight length wraps past midnight`() {
        hours("22:00", "06:30").lengthInNanos shouldBe 30_600_000_000_000
    }

    @Test
    fun `full day length is a whole day`() {
        hours("09:00", "09:00").lengthInNanos shouldBe 86_400_000_000_000
    }

    @Test
    fun `hours ending in the autumn overlap end on the second pass`() {
        hours("00:00", "01:30").occurrenceStartingOn(fallBack, LONDON) shouldBe
            occurs("2026-10-24T23:00:00Z", "2026-10-25T01:30:00Z")
    }

    private fun hours(
        start: String,
        end: String,
    ): DailyHours = DailyHours.between(LocalTime.parse(start), LocalTime.parse(end))

    private fun occurs(
        start: String,
        end: String,
    ): Occurrence = Occurrence.Occurs(interval(Instant.parse(start), Instant.parse(end)))
}
