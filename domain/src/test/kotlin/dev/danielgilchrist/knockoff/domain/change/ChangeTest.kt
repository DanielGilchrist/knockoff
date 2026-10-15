package dev.danielgilchrist.knockoff.domain.change

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ChangeTest {
    @Test
    fun `the first value is initial`() {
        Change.Pending.next("first") shouldBe Change.Initial("first")
    }

    @Test
    fun `a value after the initial one is a change from it`() {
        Change.Initial("first").next("second") shouldBe Change.Changed(before = "first", after = "second")
    }

    @Test
    fun `a value after a change is a change from its latest value`() {
        val change =
            Change
                .Changed(
                    before = "first",
                    after = "second",
                ).next("third")
                .shouldBeInstanceOf<Change.Changed<String>>()

        change.before shouldBe "second"
        change.after shouldBe "third"
    }
}
