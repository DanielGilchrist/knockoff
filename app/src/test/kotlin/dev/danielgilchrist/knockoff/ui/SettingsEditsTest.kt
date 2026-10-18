package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.ZoneId

class SettingsEditsTest {
    private val work: Rule =
        Settings.DEFAULT.rules.rules
            .single()
    private val evenings: Rule = work.copy(name = name("Evenings"))

    @Test
    fun `a new rule is appended`() {
        assertEquals(listOf(work, evenings), rulesOf(Settings.DEFAULT.withSaved(Editing.Guided.START, evenings)))
    }

    @Test
    fun `an edited rule replaces the one it came from, even when renamed`() {
        assertEquals(
            listOf(evenings),
            rulesOf(Settings.DEFAULT.withSaved(Editing.Renaming.of(work), evenings)),
        )
    }

    @Test
    fun `a new rule with a taken name is a duplicate`() {
        val duplicates =
            assertInstanceOf(
                RuleSetParse.DuplicateNames::class.java,
                Settings.DEFAULT.withSaved(Editing.Guided.START, work),
            )

        assertEquals(setOf(work.name), duplicates.names)
    }

    @Test
    fun `renaming a rule onto another rule's name is a duplicate`() {
        val settings = Settings(valid(Settings.DEFAULT.withSaved(Editing.Guided.START, evenings)), Theme.System)

        val duplicates =
            assertInstanceOf(
                RuleSetParse.DuplicateNames::class.java,
                settings.withSaved(Editing.Renaming.of(work), work.copy(name = evenings.name)),
            )

        assertEquals(setOf(evenings.name), duplicates.names)
    }

    @Test
    fun `a removed rule is gone`() {
        assertEquals(emptyList<Rule>(), rulesOf(Settings.DEFAULT.withoutRule(work.name)))
    }

    @Test
    fun `changing state touches only the named rule`() {
        val settings = Settings(valid(Settings.DEFAULT.withSaved(Editing.Guided.START, evenings)), Theme.System)

        assertEquals(
            listOf(work.copy(state = RuleState.Disabled), evenings),
            rulesOf(settings.withRuleChanged(work.name, RuleChange.SetState(RuleState.Disabled))),
        )
    }

    @Test
    fun `adding a chat already in a rule keeps one copy with the latest name`() {
        val renamed = COLLEAGUE_CHAT.copy(name = ConversationName("Renamed"))
        val held = valid(Settings.DEFAULT.withRuleChanged(work.name, RuleChange.AddChat(COLLEAGUE_CHAT)))

        val again = Settings(held, Theme.System).withRuleChanged(work.name, RuleChange.AddChat(renamed))

        assertEquals(listOf(renamed), rulesOf(again).single().chats)
    }

    private fun valid(parse: RuleSetParse): RuleSet = assertInstanceOf(RuleSetParse.Valid::class.java, parse).rules

    private fun rulesOf(parse: RuleSetParse): List<Rule> = valid(parse).rules

    private fun name(raw: String): RuleName =
        assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse(raw)).name

    @Test
    fun `edits keep the default zone`() {
        val tokyo = ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo"))
        val settings = Settings(Settings.DEFAULT.rules.withDefaultZone(tokyo), Theme.System)
        val work = settings.rules.rules.single()

        assertEquals(tokyo, valid(settings.withSaved(Editing.Guided.START, evenings)).defaultZone)
        assertEquals(tokyo, valid(settings.withoutRule(work.name)).defaultZone)
        assertEquals(
            tokyo,
            valid(settings.withRuleChanged(work.name, RuleChange.SetState(RuleState.Disabled))).defaultZone,
        )
    }
}
