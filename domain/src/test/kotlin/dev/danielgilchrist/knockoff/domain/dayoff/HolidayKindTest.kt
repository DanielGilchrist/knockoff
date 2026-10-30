package dev.danielgilchrist.knockoff.domain.dayoff

import dev.danielgilchrist.knockoff.domain.region
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HolidayKindTest {
    private val england = region("England")
    private val scotland = region("Scotland")
    private val englandWales = HolidayKind.Regional(setOf(england, region("Wales"), region("Northern Ireland")))

    @Test
    fun `descriptions are read from their first line`() {
        HolidayKind.of("Public holiday") shouldBe HolidayKind.Everywhere
        HolidayKind.of(" Public holiday \nmore") shouldBe HolidayKind.Everywhere
        HolidayKind.of("Public holiday in England, Wales, Northern Ireland") shouldBe englandWales
        HolidayKind.of("Public holiday in Scotland") shouldBe HolidayKind.Regional(setOf(scotland))
        HolidayKind.of("Observance\nTo hide observances, go to Google Calendar Settings") shouldBe
            HolidayKind.Observance
        HolidayKind.of("Public holiday in , ") shouldBe HolidayKind.Everywhere
        HolidayKind.of("Public holidays") shouldBe HolidayKind.Everywhere
        HolidayKind.of("Observances") shouldBe HolidayKind.Everywhere
        HolidayKind.of("") shouldBe HolidayKind.Everywhere
    }

    @Test
    fun `holidays everywhere are always observed, observances never, regional ones in their regions`() {
        HolidayKind.Everywhere.observedIn(emptySet()) shouldBe true
        HolidayKind.Observance.observedIn(setOf(england)) shouldBe false
        englandWales.observedIn(setOf(england)) shouldBe true
        englandWales.observedIn(setOf(scotland)) shouldBe false
        englandWales.observedIn(emptySet()) shouldBe false
    }

    @Test
    fun `holidays are the observed days, and the regions are those any event names`() {
        val events =
            listOf(
                HolidayEvent(date("2026-12-25"), HolidayKind.Everywhere),
                HolidayEvent(date("2026-08-31"), englandWales),
                HolidayEvent(date("2026-11-30"), HolidayKind.Regional(setOf(scotland))),
                HolidayEvent(date("2026-10-31"), HolidayKind.Observance),
                HolidayEvent(date("2026-10-30"), HolidayKind.Everywhere),
            )

        Holidays.of(events, setOf(england)) shouldBe
            Holidays(setOf(date("2026-12-25"), date("2026-08-31"), date("2026-10-30")))
        HolidayEvent.regionsIn(events) shouldBe englandWales.regions + scotland
    }

    @Test
    fun `an event spans each day up to its exclusive end`() {
        HolidayEvent.spanning(date("2026-12-25"), date("2026-12-26"), HolidayKind.Everywhere) shouldBe
            listOf(HolidayEvent(date("2026-12-25"), HolidayKind.Everywhere))
        HolidayEvent.spanning(date("2026-12-24"), date("2026-12-27"), HolidayKind.Observance) shouldBe
            listOf("2026-12-24", "2026-12-25", "2026-12-26").map { HolidayEvent(date(it), HolidayKind.Observance) }
        HolidayEvent.spanning(date("2026-12-25"), date("2026-12-25"), HolidayKind.Everywhere) shouldBe emptyList()
        HolidayEvent.spanning(date("2026-12-25"), date("2026-12-24"), HolidayKind.Everywhere) shouldBe emptyList()
    }

    @Test
    fun `a region can't be blank and is trimmed`() {
        HolidayRegion.parse("  ") shouldBe HolidayRegionParse.Blank
        HolidayRegion.parse(" Wales ") shouldBe HolidayRegionParse.Valid(region("Wales"))
    }

    private fun date(text: String): LocalDate = LocalDate.parse(text)
}
