package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RuleNameTest {
    @ParameterizedTest
    @ValueSource(strings = ["Work", "  Work", "Work  ", "\tWork\n"])
    fun `names are trimmed`(raw: String) {
        RuleName
            .parse(raw)
            .shouldBeInstanceOf<RuleNameParse.Valid>()
            .name.value shouldBe "Work"
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t\n"])
    fun `blank names are rejected`(raw: String) {
        RuleName.parse(raw) shouldBe RuleNameParse.Blank
    }
}
