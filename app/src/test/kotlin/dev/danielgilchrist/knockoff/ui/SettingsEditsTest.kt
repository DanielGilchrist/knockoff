package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SettingsEditsTest {
    private val work: Rule =
        Settings.DEFAULT.rules.rules
            .single()
    private val evenings: Rule = work.copy(name = name("Evenings"))

    @Test
    fun `a new rule is appended`() {
        assertEquals(listOf(work, evenings), rulesOf(Settings.DEFAULT.withRule(EditTarget.New, evenings)))
    }

    @Test
    fun `an edited rule replaces the one it came from, even when renamed`() {
        assertEquals(listOf(evenings), rulesOf(Settings.DEFAULT.withRule(EditTarget.Existing(work), evenings)))
    }

    @Test
    fun `a new rule with a taken name is a duplicate`() {
        val duplicates =
            assertInstanceOf(RuleSetParse.DuplicateNames::class.java, Settings.DEFAULT.withRule(EditTarget.New, work))

        assertEquals(setOf(work.name), duplicates.names)
    }

    @Test
    fun `renaming a rule onto another rule's name is a duplicate`() {
        val settings = Settings(valid(Settings.DEFAULT.withRule(EditTarget.New, evenings)))

        val duplicates =
            assertInstanceOf(
                RuleSetParse.DuplicateNames::class.java,
                settings.withRule(EditTarget.Existing(work), work.copy(name = evenings.name)),
            )

        assertEquals(setOf(evenings.name), duplicates.names)
    }

    @Test
    fun `a removed rule is gone`() {
        assertEquals(emptyList<Rule>(), rulesOf(Settings.DEFAULT.withoutRule(work.name)))
    }

    @Test
    fun `changing state touches only the named rule`() {
        val settings = Settings(valid(Settings.DEFAULT.withRule(EditTarget.New, evenings)))

        assertEquals(
            listOf(work.copy(state = RuleState.Disabled), evenings),
            rulesOf(settings.withRuleState(work.name, RuleState.Disabled)),
        )
    }

    private fun valid(parse: RuleSetParse): RuleSet = assertInstanceOf(RuleSetParse.Valid::class.java, parse).rules

    private fun rulesOf(parse: RuleSetParse): List<Rule> = valid(parse).rules

    private fun name(raw: String): RuleName =
        assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse(raw)).name
}
