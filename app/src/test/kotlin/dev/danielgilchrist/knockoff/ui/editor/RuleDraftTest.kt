package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.schedule.Break
import dev.danielgilchrist.knockoff.domain.schedule.BreakName
import dev.danielgilchrist.knockoff.domain.schedule.BreakNameParse
import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleDraftTest {
    private val defaultRule: Rule =
        Settings.DEFAULT.rules.rules
            .single()

    @Test
    fun `a rule survives a round trip through a draft`() {
        assertEquals(Checked.Ok(defaultRule), RuleDraft.from(defaultRule).parse())
    }

    @Test
    fun `a full draft becomes a rule with every target`() {
        val address =
            assertInstanceOf(
                AccountAddressParse.Valid::class.java,
                AccountAddress.parse("dan@work.example"),
            ).address
        val draft =
            RuleDraft.NEW.copy(
                name = "Evenings",
                state = RuleState.Disabled,
                policy = Policy.HoldDuring,
                windows = listOf(WindowDraft(setOf(DayOfWeek.SUNDAY), LocalTime.of(18, 0), LocalTime.of(8, 0))),
                apps = setOf(PackageName("com.Slack")),
                gmailAccounts = setOf(address),
                chats =
                    listOf(
                        COLLEAGUE_CHAT,
                    ).associateBy { it.key },
                breaks = listOf(lateBreak()),
            )

        val rule = valid(draft)

        assertEquals(draft, RuleDraft.from(rule))
    }

    @Test
    fun `every problem is reported at once`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "  ",
                windows = listOf(WindowDraft.WORKING_HOURS, WindowDraft.WORKING_HOURS.copy(days = emptySet())),
            )

        assertEquals(
            Checked.Problems(listOf(DraftProblem.BlankName, DraftProblem.HoursWithoutDays(2))),
            draft.parse(),
        )
    }

    @Test
    fun `breaks that cover all of the hours are reported`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                windows = listOf(WindowDraft(setOf(DayOfWeek.SUNDAY), LocalTime.of(22, 0), LocalTime.of(23, 0))),
                breaks = listOf(lateBreak()),
            )

        assertEquals(Checked.Problems(listOf(DraftProblem.BreaksCoverAllHours)), draft.parse())
    }

    @Test
    fun `a draft without hours is reported`() {
        assertEquals(
            Checked.Problems(listOf(DraftProblem.NoHours)),
            RuleDraft.NEW.copy(name = "x", windows = emptyList()).parse(),
        )
    }

    @Test
    fun `a new draft only needs a name`() {
        assertEquals(Checked.Problems(listOf(DraftProblem.BlankName)), RuleDraft.NEW.parse())
    }

    @Test
    fun `toggling a day adds then removes it`() {
        val sunday = WindowDraft.WORKING_HOURS.toggle(DayOfWeek.SUNDAY)

        assertEquals(WindowDraft.WORKING_HOURS.days + DayOfWeek.SUNDAY, sunday.days)
        assertEquals(WindowDraft.WORKING_HOURS, sunday.toggle(DayOfWeek.SUNDAY))
    }

    @Test
    fun `a valid pending gmail account is saved without pressing add`() {
        val draft = RuleDraft.NEW.copy(name = "Work mail", pendingGmail = " Dan@Work.Example ")

        val rule = valid(draft)

        assertEquals(setOf(address("dan@work.example")), RuleDraft.from(rule).gmailAccounts)
    }

    @Test
    fun `an invalid pending gmail account is a problem`() {
        assertEquals(
            Checked.Problems(listOf(DraftProblem.UnaddedAccount("Not an email address: \"dan\""))),
            RuleDraft.NEW.copy(name = "x", pendingGmail = "dan").parse(),
        )
    }

    @Test
    fun `adding a pending gmail account moves it into the set`() {
        val added = RuleDraft.NEW.copy(pendingGmail = "dan@work.example").withPendingGmailAdded()

        assertEquals(setOf(address("dan@work.example")), added.gmailAccounts)
        assertEquals("", added.pendingGmail)
    }

    @Test
    fun `adding an invalid pending gmail account changes nothing`() {
        val draft = RuleDraft.NEW.copy(pendingGmail = "dan")

        assertEquals(draft, draft.withPendingGmailAdded())
    }

    private fun address(raw: String): AccountAddress =
        assertInstanceOf(AccountAddressParse.Valid::class.java, AccountAddress.parse(raw)).address

    private fun valid(draft: RuleDraft): Rule =
        when (val checked = draft.parse()) {
            is Checked.Ok -> checked.value
            is Checked.Problems -> fail("Unexpected problems ${checked.problems}")
        }

    private fun lateBreak(): Break =
        Break(
            assertInstanceOf(BreakNameParse.Valid::class.java, BreakName.parse("Late")).name,
            Window(
                assertInstanceOf(WeekDaysParse.Valid::class.java, WeekDays.parse(setOf(DayOfWeek.SUNDAY))).days,
                DailyHours.between(LocalTime.of(21, 0), LocalTime.of(23, 30)),
            ),
        )
}
