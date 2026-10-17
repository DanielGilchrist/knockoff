package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.textPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TextPatternTest {
    @Test
    fun `a pattern matches anywhere unless anchored`() {
        textPattern("Work").matches("Team Work chat") shouldBe true
        textPattern("^Work").matches("Team Work chat") shouldBe false
        textPattern("^(Work|Team)").matches("Team standup") shouldBe true
        textPattern("Work").matches("work") shouldBe false
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t"])
    fun `a blank pattern is rejected`(source: String) {
        TextPattern.parse(source) shouldBe TextPatternParse.Blank
    }

    @Test
    fun `an invalid pattern is rejected with the reason`() {
        TextPattern.parse("(Work").shouldBeInstanceOf<TextPatternParse.Invalid>().detail shouldBe "Unclosed group"
    }

    @Test
    fun `patterns are equal when their source is`() {
        textPattern("^Work") shouldBe textPattern("^Work")
        (textPattern("^Work") == textPattern("Work")) shouldBe false
        textPattern("^Work").source shouldBe "^Work"
    }
}
