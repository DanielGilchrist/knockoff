package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class RuleSectionTest {
    private val pattern = Target.ChatNamePattern(COLLEAGUE_CHAT.packageName, textPattern("^Work"))
    private val slack = Target.App(PackageName("com.Slack"))
    private val current =
        Settings.DEFAULT.rules.rules
            .single()
            .copy(apps = RuleApps.fromTargets(setOf(COLLEAGUE_CHAT, pattern), emptySet()))
    private val edited =
        current.copy(
            name = assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse("Office")).name,
            state = RuleState.Disabled,
            policy = Policy.HoldDuring,
            apps = RuleApps.fromTargets(setOf(slack), setOf(COLLEAGUE_CHAT.packageName)),
            zone = RuleZone.Chosen(ZoneChoice.Device),
        )

    @Test
    fun `each section takes only its own fields from the edited rule`() {
        assertEquals(
            current.copy(apps = RuleApps.fromTargets(setOf(pattern, slack), edited.asking)),
            RuleSection.What.merge(current, edited),
        )
        assertEquals(current.copy(policy = Policy.HoldDuring), RuleSection.When.merge(current, edited))
        assertEquals(
            current.copy(zone = edited.zone, apps = RuleApps.fromTargets(setOf(COLLEAGUE_CHAT), emptySet())),
            RuleSection.Advanced.merge(current, edited),
        )
    }

    @Test
    fun `renaming takes only the name`() {
        assertEquals(current.copy(name = edited.name), Editing.Renaming.of(current).saving(edited))
    }

    private fun textPattern(source: String): TextPattern =
        assertInstanceOf(TextPatternParse.Valid::class.java, TextPattern.parse(source)).pattern
}
