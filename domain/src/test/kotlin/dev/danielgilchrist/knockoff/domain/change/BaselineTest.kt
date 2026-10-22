package dev.danielgilchrist.knockoff.domain.change

import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class BaselineTest {
    @Test
    fun `with no baseline the first value has nothing to reconcile`() {
        Baseline.Unknown.stepTo("now") shouldBe Step.First("now")
        Step.First("now").value shouldBe "now"
    }

    @Test
    fun `a reconciled baseline equal to the value is the same`() {
        Baseline.Known.of("now").stepTo("now") shouldBe Step.Same("now")
        Step.Same("now").value shouldBe "now"
    }

    @Test
    fun `a baseline that differs moves from it`() {
        val moved = Step.Moved(from = Baseline.Known.of("before"), to = "now")

        Baseline.Known.of("before").stepTo("now") shouldBe moved
        moved.from shouldBe Baseline.Known.of("before")
        moved.to shouldBe "now"
    }

    @Test
    fun `a skipped value is kept as a candidate until reconciled`() {
        val skipped = Baseline.Known.of("s").skipped("a")

        skipped shouldBe Baseline.Known("s", listOf("a"))
        skipped.reconciled shouldBe "s"
        skipped.since shouldBe listOf("a")
        skipped.candidates shouldBe listOf("s", "a")
    }

    @Test
    fun `going back to the reconciled value after a skip still moves`() {
        val skipped = Baseline.Known.of("s").skipped("a")

        skipped.stepTo("s") shouldBe Step.Moved(skipped, "s")
    }

    @Test
    fun `skipping a value that is already a candidate keeps the baseline as it is`() {
        val skipped = Baseline.Known.of("s").skipped("a")

        skipped.skipped("a") shouldBe skipped
        skipped.skipped("s") shouldBe skipped
        skipped.skipped("b") shouldBe Baseline.Known("s", listOf("a", "b"))
    }
}
