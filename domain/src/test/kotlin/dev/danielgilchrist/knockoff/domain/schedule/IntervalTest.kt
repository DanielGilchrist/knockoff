package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.interval
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class IntervalTest {
    private val nine = Instant.parse("2026-10-14T09:00:00Z")
    private val ten = Instant.parse("2026-10-14T10:00:00Z")
    private val eleven = Instant.parse("2026-10-14T11:00:00Z")
    private val twelve = Instant.parse("2026-10-14T12:00:00Z")
    private val nineToTen = interval(nine, ten)

    @Test
    fun `an empty or reversed interval is empty`() {
        Interval.parse(nine, nine) shouldBe IntervalParse.Empty
        Interval.parse(ten, nine) shouldBe IntervalParse.Empty
    }

    @Test
    fun `contains its start but not its end`() {
        (nine in nineToTen) shouldBe true
        (ten in nineToTen) shouldBe false
        (nine.minusNanos(1) in nineToTen) shouldBe false
        (ten.minusNanos(1) in nineToTen) shouldBe true
    }

    @Test
    fun `touches adjacent and overlapping intervals but not separate ones`() {
        nineToTen.touches(interval(ten, eleven)) shouldBe true
        interval(ten, eleven).touches(nineToTen) shouldBe true
        nineToTen.touches(interval(nine.plusSeconds(1), eleven)) shouldBe true
        nineToTen.touches(interval(eleven, twelve)) shouldBe false
        interval(eleven, twelve).touches(nineToTen) shouldBe false
    }

    @Test
    fun `span covers both intervals`() {
        nineToTen.span(interval(ten, eleven)) shouldBe interval(nine, eleven)
        interval(ten, eleven).span(nineToTen) shouldBe interval(nine, eleven)
        interval(nine, twelve).span(interval(ten, eleven)) shouldBe interval(nine, twelve)
    }
}
