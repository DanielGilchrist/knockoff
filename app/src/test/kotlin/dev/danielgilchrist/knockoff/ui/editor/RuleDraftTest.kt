package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeCoverage
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
    fun `apps to ask about come from seen and picked chats, less whole apps, plus earlier choices`() {
        val slack = PackageName("com.Slack")
        val gmail = PackageName.GMAIL
        val whatsapp = COLLEAGUE_CHAT.packageName
        val draft = RuleDraft.NEW.copy(chats = mapOf(COLLEAGUE_CHAT.key to COLLEAGUE_CHAT), asking = setOf(gmail))
        val seen = SeenChatsState.Loaded(listOf(COLLEAGUE_CHAT.copy(packageName = slack), COLLEAGUE_CHAT))

        assertEquals(setOf(slack, whatsapp, gmail), draft.askableApps(seen))
        assertEquals(setOf(whatsapp, gmail), draft.askableApps(SeenChatsState.Loading))
        assertEquals(setOf(whatsapp, gmail), draft.askableApps(SeenChatsState.Failed("x")))
        assertEquals(setOf(whatsapp, gmail), draft.copy(apps = setOf(slack, gmail)).askableApps(seen))
    }

    @Test
    fun `the apps a rule asks about survive a round trip`() {
        val asking =
            defaultRule.copy(
                apps = RuleApps.fromTargets(defaultRule.targets, setOf(COLLEAGUE_CHAT.packageName)),
            )

        assertEquals(Checked.Ok(asking), RuleDraft.from(asking).parse())
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
                patterns = listOf(PatternDraft(PackageName("com.whatsapp"), PatternField.ChatName, "^Work")),
                breaks = listOf(lateBreak()),
            )

        val rule = valid(draft)

        assertEquals(draft, RuleDraft.from(rule))
    }

    @Test
    fun `a rule's patterns survive editing`() {
        val patterns =
            listOf(
                PatternDraft(PackageName("com.whatsapp"), PatternField.ChatName, "^Work"),
                PatternDraft(PackageName("com.google.android.gm"), PatternField.ChannelId, "_dan@work\\.example$"),
            )
        val rule = valid(RuleDraft.NEW.copy(name = "x", patterns = patterns))

        assertEquals(patterns.toSet(), RuleDraft.from(rule).patterns.toSet())
        assertEquals(rule, valid(RuleDraft.from(rule)))
    }

    @Test
    fun `blank and invalid patterns are reported with their number`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                patterns =
                    listOf(
                        PatternDraft(PackageName("com.whatsapp"), PatternField.ChatName, " "),
                        PatternDraft(PackageName("com.whatsapp"), PatternField.ChannelId, "(Work"),
                    ),
            )

        assertEquals(
            Checked.Problems(listOf(DraftProblem.BlankPattern(1), DraftProblem.InvalidPattern(2, invalidDetail()))),
            draft.parse(),
        )
    }

    @Test
    fun `a pattern previews whether it is empty, invalid or valid`() {
        val app = PackageName("com.whatsapp")

        assertEquals(PatternPreview.Empty, PatternDraft(app, PatternField.ChatName, " ").preview())
        assertEquals(
            PatternPreview.Invalid(invalidDetail()),
            PatternDraft(app, PatternField.ChatName, "(Work").preview(),
        )
        assertEquals(
            PatternPreview.Valid(
                Target.ChannelPattern(
                    app,
                    assertInstanceOf(TextPatternParse.Valid::class.java, TextPattern.parse("^chats")).pattern,
                ),
            ),
            PatternDraft(app, PatternField.ChannelId, "^chats").preview(),
        )
    }

    @Test
    fun `pattern problems are reported with name and account problems`() {
        val draft =
            RuleDraft.NEW.copy(
                name = " ",
                pendingGmail = "dan",
                patterns = listOf(PatternDraft(PackageName("com.whatsapp"), PatternField.ChatName, " ")),
            )

        assertEquals(
            Checked.Problems(
                listOf(
                    DraftProblem.BlankName,
                    DraftProblem.UnaddedAccount("Not an email address: \"dan\""),
                    DraftProblem.BlankPattern(1),
                ),
            ),
            draft.parse(),
        )
    }

    @Test
    fun `the same pattern twice becomes one target`() {
        val pattern = PatternDraft(PackageName("com.whatsapp"), PatternField.ChatName, "^Work")
        val rule = valid(RuleDraft.NEW.copy(name = "x", patterns = listOf(pattern, pattern)))

        assertEquals(listOf(pattern), RuleDraft.from(rule).patterns)
    }

    @Test
    fun `coverage lists up to five seen chats and counts the rest`() {
        val five = listOf("A", "B", "C", "D", "E")

        assertEquals("Covers no chats seen so far.", describeCoverage(emptyList()))
        assertEquals("Seen chats it covers now: A, B, C, D, E", describeCoverage(five))
        assertEquals("Seen chats it covers now: A, B, C, D, E and 1 more", describeCoverage(five + "F"))
    }

    @Test
    fun `pattern problems are described`() {
        assertEquals("Pattern 1 is empty.", DraftProblem.BlankPattern(1).describe())
        assertEquals(
            "Pattern 2 isn't a valid regular expression: Unclosed group",
            DraftProblem.InvalidPattern(2, "Unclosed group").describe(),
        )
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
    fun `a break without a name or days is reported with its number`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                breaks = listOf(BreakDraft.LUNCH, BreakDraft(" ", BreakDraft.LUNCH.window.copy(days = emptySet()))),
            )

        assertEquals(
            Checked.Problems(listOf(DraftProblem.BlankBreakName(2), DraftProblem.BreakWithoutDays(2))),
            draft.parse(),
        )
    }

    @Test
    fun `hour and break problems are reported together`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                windows = listOf(WindowDraft.WORKING_HOURS.copy(days = emptySet())),
                breaks = listOf(BreakDraft.LUNCH.copy(window = BreakDraft.LUNCH.window.copy(days = emptySet()))),
            )

        assertEquals(
            Checked.Problems(listOf(DraftProblem.HoursWithoutDays(1), DraftProblem.BreakWithoutDays(1))),
            draft.parse(),
        )
    }

    @Test
    fun `an added break is a lunch on the days of the hours`() {
        val weekends =
            WindowDraft(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), LocalTime.of(10, 0), LocalTime.of(16, 0))
        val mondays = WindowDraft(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        val union = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

        assertEquals(
            listOf(BreakDraft.LUNCH.copy(window = BreakDraft.LUNCH.window.copy(days = union))),
            RuleDraft.NEW
                .copy(windows = listOf(weekends, mondays))
                .withBreakAdded()
                .breaks,
        )
        assertEquals(
            listOf(BreakDraft.LUNCH),
            RuleDraft.NEW
                .copy(windows = emptyList())
                .withBreakAdded()
                .breaks,
        )
    }

    @Test
    fun `missing hours are reported with break problems`() {
        assertEquals(
            Checked.Problems(listOf(DraftProblem.NoHours, DraftProblem.BlankBreakName(1))),
            RuleDraft.NEW
                .copy(
                    name = "x",
                    windows = emptyList(),
                    breaks = listOf(BreakDraft.LUNCH.copy(name = " ")),
                ).parse(),
        )
    }

    @Test
    fun `a full-day break survives a round trip`() {
        val sunday = BreakDraft("Sunday", WindowDraft(setOf(DayOfWeek.SUNDAY), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT))
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                windows = listOf(WindowDraft(DayOfWeek.entries.toSet(), LocalTime.of(8, 0), LocalTime.of(17, 0))),
                breaks = listOf(sunday),
            )

        assertEquals(listOf(sunday), RuleDraft.from(valid(draft)).breaks)
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

    private fun lateBreak(): BreakDraft =
        BreakDraft("Late", WindowDraft(setOf(DayOfWeek.SUNDAY), LocalTime.of(21, 0), LocalTime.of(23, 30)))

    private fun invalidDetail(): String =
        assertInstanceOf(TextPatternParse.Invalid::class.java, TextPattern.parse("(Work")).detail
}
