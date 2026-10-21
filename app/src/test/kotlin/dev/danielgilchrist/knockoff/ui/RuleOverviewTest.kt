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

        assertEquals("Holds nothing yet. Add an app under Apps.", overview.sentence)
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
            "Lets notifications from 1 app, 1 chat and 1 pattern through only on weekdays 08:00–17:00, except Lunch.",
            overview.sentence,
        )
        assertEquals("Slack, WhatsApp", overview.apps)
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
            "Holds notifications from 4 apps on weekends 08:00–17:00 and every day 08:00–17:00 and on " +
                "Mon, Wed 08:00–17:00.",
            overview.sentence,
        )
        assertEquals("Weekends 08:00–17:00; Every day 08:00–17:00; Mon, Wed 08:00–17:00", overview.hours)
        assertEquals("Slack, b, c and 1 more", overview.apps)
    }

    @Test
    fun `an off rule says so and an asking-only rule says what it asks about`() {
        val asking = AppDraft(AppMode.Narrowed, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.Ask)
        val draft = RuleDraft.NEW.copy(name = "x", state = RuleState.Disabled, apps = mapOf(slack to asking))

        assertEquals(
            "Off. Holds nothing yet. Asks about new chats from 1 app.",
            RuleOverview.of(rule(draft), labelOf).sentence,
        )
        assertEquals(
            "Holds nothing yet. Asks about new chats from 1 app.",
            RuleOverview.of(rule(draft), labelOf).effect,
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
            "Lets notifications from 1 account and 2 groups through only on weekdays 08:00–17:00.",
            RuleOverview.of(rule(draft), { it.value }).sentence,
        )
    }

    private fun rule(draft: RuleDraft): Rule =
        when (val checked = draft.parse()) {
            is Checked.Ok -> checked.value
            is Checked.Problems -> error("Unexpected problems ${checked.problems}")
        }
}
