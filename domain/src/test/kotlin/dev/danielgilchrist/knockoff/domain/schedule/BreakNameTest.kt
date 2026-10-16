package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BreakNameTest {
    @ParameterizedTest
    @ValueSource(strings = ["Lunch", " Lunch ", "\tLunch\n"])
    fun `names are trimmed`(raw: String) {
        BreakName
            .parse(raw)
            .shouldBeInstanceOf<BreakNameParse.Valid>()
            .name.value shouldBe "Lunch"
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t\n"])
    fun `blank names are rejected`(raw: String) {
        BreakName.parse(raw) shouldBe BreakNameParse.Blank
    }
}
