package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ui.editor.AppDraft
import dev.danielgilchrist.knockoff.ui.editor.AppMode
import dev.danielgilchrist.knockoff.ui.editor.BreakDraft
import dev.danielgilchrist.knockoff.ui.editor.Checked
import dev.danielgilchrist.knockoff.ui.editor.PatternDraft
import dev.danielgilchrist.knockoff.ui.editor.PatternField
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import dev.danielgilchrist.knockoff.ui.editor.WindowDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.ZoneId

class RuleOverviewTest {
    private val slack = PackageName("com.Slack")
    private val labels = mapOf(slack to "Slack", COLLEAGUE_CHAT.packageName to "WhatsApp")
    private val labelOf = { app: PackageName -> labels.getValue(app) }

    @Test
    fun `an empty rule says it holds nothing yet`() {
        val overview = RuleOverview.of(rule(RuleDraft.NEW.copy(name = "x")), labelOf)

        assertEquals("No apps yet. Add one under Apps.", overview.sentence)
        assertEquals("No apps yet", overview.apps)
        assertEquals("Weekdays 08:00–17:00", overview.hours)
    }

    @Test
    fun `a full rule reads as one sentence with its details below`() {
        val draft =
            RuleDraft.NEW.copy(
                name = "Work",
                apps =
                    mapOf(
                        slack to AppDraft.EVERYTHING,
                        COLLEAGUE_CHAT.packageName to
                            AppDraft(
                                AppMode.Narrowed,
                                emptyMap(),
                                mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT)),
                                listOf(PatternDraft(PatternField.ChatName, "^Work")),
                                NewChatsAsking.Ask,
                            ),
                    ),
                breaks = listOf(BreakDraft.LUNCH),
                zone = RuleZone.Chosen(ZoneChoice.Fixed(ZoneId.of("Etc/UTC"))),
            )
        val overview = RuleOverview.of(rule(draft), labelOf)

        assertEquals(
            "Slack and some WhatsApp chats only reach you on weekdays 08:00–17:00, except Lunch. " +
                "You'll be asked about new WhatsApp chats.",
            overview.sentence,
        )
        assertEquals("Slack and WhatsApp", overview.apps)
        assertEquals(overview.sentence, overview.effect)
        assertEquals("Weekdays 08:00–17:00 · except Lunch · Etc/UTC (UTC+00:00)", overview.hours)
    }

    @Test
    fun `holding rules and day sets read naturally`() {
        val weekend = WindowDraft.WORKING_HOURS.copy(days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        val everyDay = WindowDraft.WORKING_HOURS.copy(days = DayOfWeek.entries.toSet())
        val some = WindowDraft.WORKING_HOURS.copy(days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val draft =
            RuleDraft.NEW.copy(
                name = "Home",
                policy = Policy.HoldDuring,
                apps =
                    listOf(
                        slack,
                        PackageName("b"),
                        PackageName("c"),
                        PackageName("d"),
                    ).associateWith { AppDraft.EVERYTHING },
                windows = listOf(weekend, everyDay, some),
            )
        val overview = RuleOverview.of(rule(draft), { if (it == slack) "Slack" else it.value })

        assertEquals(
            "Slack, b, c and 1 more go quiet on weekends 08:00–17:00 and every day 08:00–17:00 and on " +
                "Mon, Wed 08:00–17:00.",
            overview.sentence,
        )
        assertEquals("Weekends 08:00–17:00; Every day 08:00–17:00; Mon, Wed 08:00–17:00", overview.hours)
        assertEquals("Slack, b, c and 1 more", overview.apps)
    }

    @Test
    fun `an asking-only rule says what it asks about only while it's on`() {
        val asking = AppDraft(AppMode.Narrowed, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.Ask)
        val draft = RuleDraft.NEW.copy(name = "x", apps = mapOf(slack to asking))
        val off = draft.copy(state = RuleState.Disabled)

        assertEquals(
            "Nothing picked yet. You'll be asked about new Slack chats.",
            RuleOverview.of(rule(draft), labelOf).sentence,
        )
        assertEquals("Off. Nothing picked yet.", RuleOverview.of(rule(off), labelOf).sentence)
        assertEquals("Nothing picked yet.", RuleOverview.of(rule(off), labelOf).effect)
    }

    @Test
    fun `an off rule holding apps doesn't promise to ask`() {
        val asking = AppDraft(AppMode.Narrowed, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.Ask)
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                state = RuleState.Disabled,
                apps = mapOf(slack to AppDraft.EVERYTHING, COLLEAGUE_CHAT.packageName to asking),
            )

        assertEquals(
            "Off. Slack only reaches you on weekdays 08:00–17:00.",
            RuleOverview.of(rule(draft), labelOf).sentence,
        )
    }

    @Test
    fun `a sentence led by a pattern-only app opens with a capital`() {
        val patterned =
            AppDraft(
                AppMode.Narrowed,
                emptyMap(),
                emptyMap(),
                listOf(PatternDraft(PatternField.ChatName, "^Work")),
                NewChatsAsking.DontAsk,
            )
        val draft = RuleDraft.NEW.copy(name = "x", apps = mapOf(COLLEAGUE_CHAT.packageName to patterned))

        assertEquals(
            "Some WhatsApp chats only reach you on weekdays 08:00–17:00.",
            RuleOverview.of(rule(draft), labelOf).sentence,
        )
        assertEquals(
            "Some WhatsApp chats go quiet on weekdays 08:00–17:00.",
            RuleOverview.of(rule(draft.copy(policy = Policy.HoldDuring)), labelOf).sentence,
        )
        assertEquals(
            "Off. Some WhatsApp chats and Slack only reach you on weekdays 08:00–17:00.",
            RuleOverview
                .of(
                    rule(draft.copy(state = RuleState.Disabled, apps = draft.apps + (slack to AppDraft.EVERYTHING))),
                    labelOf,
                ).sentence,
        )
    }

    @Test
    fun `held groups are called accounts only when their app's names are addresses`() {
        val mail = PackageName("com.google.android.gm")
        val accounts =
            AppDraft.EVERYTHING.copy(
                mode = AppMode.Narrowed,
                accounts = mapOf(ChannelGroupId("a") to groupName("dan@work.example")),
            )
        val groups =
            AppDraft.EVERYTHING.copy(
                mode = AppMode.Narrowed,
                accounts =
                    mapOf(ChannelGroupId("b") to groupName("Chats"), ChannelGroupId("c") to groupName("Calls")),
            )
        val draft = RuleDraft.NEW.copy(name = "x", apps = mapOf(mail to accounts, slack to groups))

        assertEquals(
            "dan@work.example in com.google.android.gm and 2 com.Slack groups only reach you on weekdays " +
                "08:00–17:00.",
            RuleOverview.of(rule(draft), { it.value }).sentence,
        )
    }

    @Test
    fun `one held thing reads in the singular and a holding rule goes quiet`() {
        val boss =
            RuleDraft.NEW.copy(
                name = "x",
                apps =
                    mapOf(COLLEAGUE_CHAT.packageName to chats(NewChatsAsking.DontAsk)),
            )
        val holding =
            RuleDraft.NEW.copy(
                name = "x",
                policy = Policy.HoldDuring,
                apps =
                    mapOf(slack to AppDraft.EVERYTHING),
            )

        assertEquals(
            "Colleague on WhatsApp only reaches you on weekdays 08:00–17:00.",
            RuleOverview.of(rule(boss), labelOf).effect,
        )
        assertEquals("Slack goes quiet on weekdays 08:00–17:00.", RuleOverview.of(rule(holding), labelOf).effect)
        assertEquals(
            "Slack and Colleague on WhatsApp go quiet on weekdays 08:00–17:00.",
            RuleOverview.of(rule(holding.copy(apps = holding.apps + boss.apps)), labelOf).effect,
        )
    }

    @Test
    fun `apps that only ask are still mentioned when others are held`() {
        val asking = AppDraft(AppMode.Narrowed, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.Ask)
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                apps =
                    mapOf(
                        slack to AppDraft.EVERYTHING,
                        COLLEAGUE_CHAT.packageName to asking,
                    ),
            )

        assertEquals(
            "Slack only reaches you on weekdays 08:00–17:00. You'll be asked about new WhatsApp chats.",
            RuleOverview.of(rule(draft), labelOf).effect,
        )
        assertEquals(
            "Slack goes quiet on weekdays 08:00–17:00. You'll be asked about new WhatsApp chats.",
            RuleOverview.of(rule(draft.copy(policy = Policy.HoldDuring)), labelOf).effect,
        )
    }

    @Test
    fun `asking about several apps lists them`() {
        val asking = AppDraft(AppMode.Narrowed, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.Ask)
        val draft = RuleDraft.NEW.copy(name = "x", apps = mapOf(COLLEAGUE_CHAT.packageName to asking, slack to asking))

        assertEquals(
            "Nothing picked yet. You'll be asked about new WhatsApp and Slack chats.",
            RuleOverview.of(rule(draft), labelOf).effect,
        )
    }

    @Test
    fun `a combined phrase mid-list keeps its app with it`() {
        val mail = PackageName.GMAIL
        val combined =
            AppDraft(
                AppMode.Narrowed,
                mapOf(ChannelGroupId("a") to groupName("dan@work.example")),
                mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT.copy(packageName = mail))),
                emptyList(),
                NewChatsAsking.DontAsk,
            )
        val draft =
            RuleDraft.NEW.copy(
                name = "x",
                apps =
                    mapOf(
                        slack to AppDraft.EVERYTHING,
                        mail to combined,
                        COLLEAGUE_CHAT.packageName to chats(NewChatsAsking.DontAsk),
                    ),
            )

        assertEquals(
            "Slack, 1 account plus 1 chat in Gmail and Colleague on WhatsApp only reach you on weekdays " +
                "08:00–17:00.",
            RuleOverview.of(rule(draft), labels.plus(mail to "Gmail")::getValue).effect,
        )
    }

    private fun chats(asking: NewChatsAsking): AppDraft =
        AppDraft(
            AppMode.Narrowed,
            emptyMap(),
            mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT)),
            emptyList(),
            asking,
        )

    private fun rule(draft: RuleDraft): Rule =
        when (val checked = draft.parse()) {
            is Checked.Ok -> checked.value
            is Checked.Problems -> error("Unexpected problems ${checked.problems}")
        }
}
