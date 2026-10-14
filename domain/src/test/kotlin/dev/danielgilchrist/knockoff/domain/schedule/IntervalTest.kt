package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldThrow
import org.junit.jupiter.api.Test
import java.time.Instant

class IntervalTest {
    private val nine = Instant.parse("2026-10-14T09:00:00Z")
    private val ten = Instant.parse("2026-10-14T10:00:00Z")
    private val eleven = Instant.parse("2026-10-14T11:00:00Z")
    private val twelve = Instant.parse("2026-10-14T12:00:00Z")
    private val nineToTen = Interval(nine, ten)

    @Test
    fun `rejects an empty or reversed interval`() {
        shouldThrow<IllegalArgumentException> { Interval(nine, nine) }
        shouldThrow<IllegalArgumentException> { Interval(ten, nine) }
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
        nineToTen.touches(Interval(ten, eleven)) shouldBe true
        Interval(ten, eleven).touches(nineToTen) shouldBe true
        nineToTen.touches(Interval(nine.plusSeconds(1), eleven)) shouldBe true
        nineToTen.touches(Interval(eleven, twelve)) shouldBe false
        Interval(eleven, twelve).touches(nineToTen) shouldBe false
    }

    @Test
    fun `span covers both intervals`() {
        nineToTen.span(Interval(ten, eleven)) shouldBe Interval(nine, eleven)
        Interval(ten, eleven).span(nineToTen) shouldBe Interval(nine, eleven)
        Interval(nine, twelve).span(Interval(ten, eleven)) shouldBe Interval(nine, twelve)
    }
}
