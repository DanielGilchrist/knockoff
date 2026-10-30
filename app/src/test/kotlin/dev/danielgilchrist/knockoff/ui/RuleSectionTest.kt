package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
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
import dev.danielgilchrist.knockoff.ruleApps
import dev.danielgilchrist.knockoff.ui.editor.AppDraft
import dev.danielgilchrist.knockoff.ui.editor.AppMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RuleSectionTest {
    private val pattern = Target.ChatNamePattern(COLLEAGUE_CHAT.packageName, textPattern("^Work"))
    private val slack = Target.App(PackageName("com.Slack"))
    private val current =
        Settings.DEFAULT.rules.rules
            .single()
            .copy(apps = ruleApps(setOf(COLLEAGUE_CHAT, pattern), emptySet()))
    private val edited =
        current.copy(
            name = assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse("Office")).name,
            state = RuleState.Disabled,
            policy = Policy.HoldDuring,
            apps = ruleApps(setOf(slack), setOf(COLLEAGUE_CHAT.packageName)),
            zone = RuleZone.Chosen(ZoneChoice.Device),
            daysOff = DaysOff(listOf(Leave.day(LocalDate.parse("2026-10-30"))), PublicHolidays.Off),
        )

    @Test
    fun `each section takes only its own fields from the edited rule`() {
        assertEquals(current.copy(apps = edited.apps), RuleSection.Apps.merge(current, current, edited))
        assertEquals(
            current.copy(policy = Policy.HoldDuring, zone = edited.zone),
            RuleSection.Hours.merge(current, current, edited),
        )
        assertEquals(current.copy(daysOff = edited.daysOff), RuleSection.DaysOff.merge(current, current, edited))
    }

    @Test
    fun `saving apps keeps chats the rule was given while the section was open`() {
        val friend = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("Friend"))
        val stored = current.copy(apps = current.apps.withChat(friend))

        assertEquals(
            setOf(slack, friend),
            RuleSection.Apps.merge(current, stored, edited).targets,
        )
    }

    @Test
    fun `a section only counts as changed when what it would save differs`() {
        val whole =
            Editing.Section.of(
                current.copy(apps = ruleApps(setOf(slack), emptySet())),
                RuleSection.Apps,
            )
        val leftovers = AppDraft.EVERYTHING.copy(chats = mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT)))

        assertEquals(false, whole.changed)
        assertEquals(false, whole.copy(draft = whole.draft.withApp(slack.packageName, leftovers)).changed)
        assertEquals(false, whole.copy(draft = whole.draft.copy(name = "Office")).changed)
        assertEquals(true, whole.copy(draft = whole.draft.withoutApp(slack.packageName)).changed)
        assertEquals(true, whole.copy(draft = whole.draft.withApp(slack.packageName, blank)).changed)
        assertEquals(false, whole.copy(draft = whole.draft.withApp(slack.packageName, AppDraft.EVERYTHING)).changed)
    }

    @Test
    fun `renaming takes only the name`() {
        assertEquals(current.copy(name = edited.name), Editing.Renaming.of(current).saving(edited))
    }

    private val blank = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)

    private fun textPattern(source: String): TextPattern =
        assertInstanceOf(TextPatternParse.Valid::class.java, TextPattern.parse(source)).pattern
}
