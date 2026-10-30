package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.LONDON
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.known
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.traits
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import org.junit.jupiter.api.Test
import java.time.ZoneId

class GroupSummaryTest {
    private val evening = london("2026-10-14T20:00").toInstant()
    private val work =
        Evaluation(
            ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT))),
            LONDON,
            Holidays.NONE,
        )
    private val summary = post("summary", Conversation.Routed(COLLEAGUE))
    private val colleague = post("colleague", known(COLLEAGUE))
    private val friend = post("friend", known(FRIEND))

    @Test
    fun `a held summary with nothing else showing is held`() {
        work.decide(summary, GroupRole.Summary(emptyList()), evening).shouldBeInstanceOf<Decision.Snooze>()
    }

    @Test
    fun `a held summary over only held notifications is held`() {
        work.decide(summary, GroupRole.Summary(listOf(colleague)), evening).shouldBeInstanceOf<Decision.Snooze>()
    }

    @Test
    fun `a held summary stays while anything under it stays showing`() {
        work.decide(summary, GroupRole.Summary(listOf(colleague, friend)), evening) shouldBe
            Decision.Leave(LeaveReason.GroupShowing)
    }

    @Test
    fun `a held summary stays over a notification that can't be snoozed`() {
        val ongoing =
            PostedNotification.Unsnoozable(
                NotificationKey("call"),
                whatsappOrigin(known(COLLEAGUE)),
                traits(NotificationTrait.Ongoing),
            )

        work.decide(summary, GroupRole.Summary(listOf(ongoing)), evening) shouldBe
            Decision.Leave(LeaveReason.GroupShowing)
    }

    @Test
    fun `a summary its rules leave is left whatever is under it`() {
        val unheld = post("summary", Conversation.Routed(FRIEND))

        work.decide(unheld, GroupRole.Summary(listOf(colleague)), evening) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `a member is decided on its own`() {
        work.decide(colleague, GroupRole.Member, evening).shouldBeInstanceOf<Decision.Snooze>()
        work.decide(friend, GroupRole.Member, evening) shouldBe Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `a summary is rechecked with what shows under it`() {
        val none = Evaluation(ruleSet(), LONDON, Holidays.NONE)

        work
            .recheck(
                summary,
                GroupRole.Summary(emptyList()),
                Baseline.Known.of(none),
                evening,
            ).shouldBeInstanceOf<Recheck.Resnooze>()
        work.recheck(summary, GroupRole.Summary(listOf(friend)), Baseline.Known.of(none), evening) shouldBe
            Recheck.Unchanged
        none
            .recheck(
                summary,
                GroupRole.Summary(emptyList()),
                Baseline.Known.of(work),
                evening,
            ).shouldBeInstanceOf<Recheck.Release>()
        work.recheck(summary, GroupRole.Summary(listOf(friend)), Baseline.Known.of(work), evening) shouldBe
            Recheck.Unchanged
    }

    @Test
    fun `a summary left for what shows under it says so`() {
        Decision.Leave(LeaveReason.GroupShowing).describe() shouldBe
            "leave: a summary over notifications that stay showing"
    }

    private fun post(
        key: String,
        conversation: Conversation,
    ): PostedNotification = PostedNotification.Snoozable(NotificationKey(key), whatsappOrigin(conversation))

    @Test
    fun `going back after a skipped change releases what only the skipped rules held`() {
        val none = Evaluation(ruleSet(), LONDON, Holidays.NONE)
        val skipped = Baseline.Known.of(none).skipped(work)

        none.recheck(colleague, GroupRole.Member, skipped, evening).shouldBeInstanceOf<Recheck.Release>()
        none.recheck(friend, GroupRole.Member, skipped, evening) shouldBe Recheck.Unchanged
    }

    @Test
    fun `a change after a skipped one treats what the skipped rules held as held`() {
        val none = Evaluation(ruleSet(), LONDON, Holidays.NONE)
        val elsewhere = Evaluation(ruleSet(), ZoneId.of("Europe/Paris"), Holidays.NONE)
        val skipped = Baseline.Known.of(none).skipped(work)

        elsewhere.recheck(colleague, GroupRole.Member, skipped, evening).shouldBeInstanceOf<Recheck.Release>()
        work.recheck(colleague, GroupRole.Member, skipped, evening).shouldBeInstanceOf<Recheck.Resnooze>()
        work.recheck(colleague, GroupRole.Member, Baseline.Known.of(work), evening) shouldBe Recheck.Unchanged
    }
}
