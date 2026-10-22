package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ruleApps
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeCoverage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleDraftTest {
    private val slack = PackageName("com.Slack")
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val workMail =
        AppDraft(
            AppMode.Narrowed,
            mapOf(AccountId.Group(ChannelGroupId("^ncg_1_account_dan@work.example")) to groupName("dan@work.example")),
            emptyMap(),
            emptyList(),
            NewChatsAsking.DontAsk,
        )
    private val colleague =
        AppDraft(
            AppMode.Narrowed,
            emptyMap(),
            mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT)),
            listOf(PatternDraft(PatternField.ChatName, "^Work")),
            NewChatsAsking.Ask,
        )
    private val defaultRule: Rule =
        Settings.DEFAULT.rules.rules
            .single()

    @Test
    fun `a rule survives a round trip through a draft`() {
        assertEquals(Checked.Ok(defaultRule), RuleDraft.from(defaultRule).parse())
    }

    @Test
    fun `the apps a rule asks about survive a round trip`() {
        val asking =
            defaultRule.copy(
                apps = ruleApps(defaultRule.targets, setOf(COLLEAGUE_CHAT.packageName)),
            )

        assertEquals(Checked.Ok(asking), RuleDraft.from(asking).parse())
    }

    @Test
    fun `a full draft becomes a rule with every kind of app`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "Evenings",
                state = RuleState.Disabled,
                policy = Policy.HoldDuring,
                windows = listOf(WindowDraft(setOf(DayOfWeek.SUNDAY), LocalTime.of(18, 0), LocalTime.of(8, 0))),
                apps = mapOf(slack to AppDraft.EVERYTHING, PackageName.GMAIL to workMail, whatsapp to colleague),
                breaks = listOf(lateBreak()),
            )

        assertEquals(draft, RuleDraft.from(valid(draft)))
    }

    @Test
    fun `blank and invalid patterns are reported with their app and number`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                apps =
                    mapOf(
                        whatsapp to
                            colleague.copy(
                                patterns =
                                    listOf(
                                        PatternDraft(PatternField.ChatName, " "),
                                        PatternDraft(PatternField.ChannelId, "(Work"),
                                    ),
                            ),
                    ),
            )

        assertEquals(
            Checked.Problems(
                listOf(
                    DraftProblem.BlankPattern(whatsapp, 1),
                    DraftProblem.InvalidPattern(whatsapp, 2, invalidDetail()),
                ),
            ),
            draft.parse(),
        )
    }

    @Test
    fun `a pattern previews whether it is empty, invalid or valid`() {
        assertEquals(PatternPreview.Empty, PatternDraft(PatternField.ChatName, " ").preview())
        assertEquals(PatternPreview.Invalid(invalidDetail()), PatternDraft(PatternField.ChatName, "(Work").preview())
        assertEquals(
            PatternPreview.Valid(
                PatternPick.Channel(
                    assertInstanceOf(TextPatternParse.Valid::class.java, TextPattern.parse("^chats")).pattern,
                ),
            ),
            PatternDraft(PatternField.ChannelId, "^chats").preview(),
        )
        assertEquals(PatternDraft.BLANK, PatternDraft(PatternField.ChatName, ""))
    }

    @Test
    fun `pattern problems are reported with name problems`() {
        val draft =
            RuleDraft.NEW.copy(
                name = " ",
                apps = mapOf(whatsapp to colleague.copy(patterns = listOf(PatternDraft(PatternField.ChatName, " ")))),
            )

        assertEquals(
            Checked.Problems(listOf(DraftProblem.BlankName, DraftProblem.BlankPattern(whatsapp, 1))),
            draft.parse(),
        )
    }

    @Test
    fun `the same pattern twice becomes one`() {
        val pattern = PatternDraft(PatternField.ChatName, "^Work")
        val rule =
            valid(
                RuleDraft.NEW.copy(
                    name = "x",
                    apps =
                        mapOf(whatsapp to colleague.copy(patterns = listOf(pattern, pattern))),
                ),
            )

        assertEquals(
            listOf(pattern),
            RuleDraft
                .from(rule)
                .apps
                .getValue(whatsapp)
                .patterns,
        )
    }

    @Test
    fun `an app narrowed to nothing is a problem`() {
        val nothing = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)

        assertEquals(
            Checked.Problems(listOf(DraftProblem.NothingChosen(whatsapp))),
            RuleDraft.NEW.copy(name = "x", apps = mapOf(whatsapp to nothing)).parse(),
        )
    }

    @Test
    fun `an app held whole keeps its picks while editing but saves without them`() {
        val whole = colleague.copy(mode = AppMode.Everything)
        val rule = valid(RuleDraft.NEW.copy(name = "x", apps = mapOf(whatsapp to whole)))

        assertEquals(mapOf(whatsapp to AppDraft.EVERYTHING), RuleDraft.from(rule).apps)
    }

    @Test
    fun `apps are added whole, kept once and removed`() {
        val draft = RuleDraft.NEW.copy(apps = mapOf(whatsapp to colleague))

        assertEquals(
            mapOf(whatsapp to colleague, slack to AppDraft.EVERYTHING),
            draft.withApps(setOf(whatsapp, slack)).apps,
        )
        assertEquals(mapOf(whatsapp to workMail), draft.withApp(whatsapp, workMail).apps)
        assertEquals(emptyMap<PackageName, AppDraft>(), draft.withoutApp(whatsapp).apps)
    }

    @Test
    fun `saving names accounts by what was learned for their app`() {
        val id = ChannelGroupId("^ncg_1_account_dan@work.example")
        val bare = workMail.copy(accounts = mapOf(AccountId.Group(id) to GroupName.of(id)))
        val groups = KnownGroups.NONE.read(whatsapp, emptyMap(), mapOf(id to groupName("dan@work.example")))
        val draft = RuleDraft.NEW.copy(apps = mapOf(whatsapp to bare, slack to bare))

        assertEquals(
            mapOf(whatsapp to workMail, slack to bare),
            draft.withLearnedNames(groups).apps,
        )
    }

    @Test
    fun `coverage lists up to five seen chats and counts the rest`() {
        val five = listOf("A", "B", "C", "D", "E")

        assertEquals("Covers no chats seen so far.", describeCoverage(emptyList()))
        assertEquals("Seen chats it covers now: A, B, C, D, E", describeCoverage(five))
        assertEquals("Seen chats it covers now: A, B, C, D, E and 1 more", describeCoverage(five + "F"))
    }

    @Test
    fun `app problems are described with the app's name`() {
        val labelOf = { app: PackageName -> if (app == whatsapp) "WhatsApp" else app.value }

        val pickOf = { _: PackageName -> "chat" }

        assertEquals(
            "WhatsApp pattern 1 is empty.",
            DraftProblem.BlankPattern(whatsapp, 1).describe(labelOf, pickOf),
        )
        assertEquals(
            "WhatsApp pattern 2 isn't a valid regular expression: Unclosed group",
            DraftProblem.InvalidPattern(whatsapp, 2, "Unclosed group").describe(labelOf, pickOf),
        )
        assertEquals(
            "Pick a chat for WhatsApp, or choose All notifications.",
            DraftProblem.NothingChosen(whatsapp).describe(labelOf, pickOf),
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
