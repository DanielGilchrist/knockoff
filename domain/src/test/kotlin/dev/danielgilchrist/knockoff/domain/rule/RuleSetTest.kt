package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.PERSONAL_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.known
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.textPattern
import dev.danielgilchrist.knockoff.domain.traits
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import dev.danielgilchrist.knockoff.domain.window
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration

class RuleSetTest {
    private val key = NotificationKey("0|com.Slack|1|null|10001")
    private val evening = london("2026-10-14T20:00")
    private val work = setOf(Target.App(SLACK), Target.GmailInbox(WORK_ADDRESS))
    private val workOnly = rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, work)
    private val lunch = schedule(window(EVERY_DAY, "12:00", "13:00"))
    private val untilMorning =
        Decision.Snooze(
            evening,
            london("2026-10-15T08:00"),
            SnoozeEnd.RulesPermit,
            setOf(ruleName("Work")),
        )

    @Test
    fun `work app outside hours snoozes until work starts`() {
        ruleSet(workOnly).decide(post(SLACK_ORIGIN), evening) shouldBe untilMorning
    }

    @Test
    fun `work gmail outside hours snoozes until work starts`() {
        ruleSet(workOnly).decide(post(WORK_GMAIL_ORIGIN), evening) shouldBe untilMorning
    }

    @Test
    fun `personal gmail outside hours is left`() {
        ruleSet(workOnly).decide(post(PERSONAL_GMAIL_ORIGIN), evening) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `non-work app outside hours is left`() {
        ruleSet(
            workOnly,
        ).decide(post(Origin(WHATSAPP, Channel.Unnamed, Conversation.None, OriginGroup.Unknown)), evening) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `a held chat outside hours snoozes and other chats in the app are left`() {
        val chats = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT)))

        chats.decide(post(whatsappOrigin(known(COLLEAGUE))), evening) shouldBe untilMorning
        chats.decide(post(whatsappOrigin(known(FRIEND))), evening) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `chats whose name matches a pattern are held and others are left`() {
        val named =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))),
                ),
            )

        named.decide(post(whatsappOrigin(known(FRIEND, "Work – Sydney"))), evening) shouldBe untilMorning
        named.decide(post(whatsappOrigin(known(FRIEND, "Mum"))), evening) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `ongoing work notification is left with its traits`() {
        val traits = traits(NotificationTrait.Ongoing, NotificationTrait.ForegroundService)

        ruleSet(workOnly).decide(PostedNotification.Unsnoozable(key, SLACK_ORIGIN, traits), evening) shouldBe
            Decision.Leave(LeaveReason.Unsnoozable(traits))
    }

    @Test
    fun `work app inside hours is permitted`() {
        ruleSet(workOnly).decide(post(SLACK_ORIGIN), london("2026-10-14T10:00")) shouldBe
            Decision.Leave(LeaveReason.Permitted(by = setOf(ruleName("Work"))))
    }

    @Test
    fun `no rules leaves everything`() {
        ruleSet().decide(post(SLACK_ORIGIN), evening) shouldBe Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `disabled and untargeted rules leave the notification`() {
        val rules =
            ruleSet(
                rule("Off", Policy.DeliverOnlyDuring, WORK_HOURS, work, RuleState.Disabled),
                rule("Other", Policy.DeliverOnlyDuring, WORK_HOURS, emptySet()),
            )

        rules.decide(post(SLACK_ORIGIN), evening) shouldBe Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `every permitting rule is named`() {
        val quietLunch = rule("Lunch", Policy.HoldDuring, lunch, work)

        ruleSet(workOnly, quietLunch).decide(post(SLACK_ORIGIN), london("2026-10-14T10:00")) shouldBe
            Decision.Leave(LeaveReason.Permitted(by = setOf(ruleName("Work"), ruleName("Lunch"))))
    }

    @Test
    fun `a hold outranks a permit`() {
        val quietLunch = rule("Lunch", Policy.HoldDuring, lunch, work)
        val now = london("2026-10-14T12:15")

        ruleSet(workOnly, quietLunch).decide(post(SLACK_ORIGIN), now) shouldBe
            Decision.Snooze(now, london("2026-10-14T13:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Lunch")))
    }

    @Test
    fun `release waits until every rule permits at the same moment`() {
        val earlyMeetings =
            rule(
                "Early meetings",
                Policy.HoldDuring,
                schedule(window(EVERY_DAY, "06:00", "07:00"), window(EVERY_DAY, "08:00", "09:00")),
                work,
            )
        val now = london("2026-10-14T05:00")

        ruleSet(workOnly, earlyMeetings).decide(post(SLACK_ORIGIN), now) shouldBe
            Decision.Snooze(
                now,
                london("2026-10-14T09:00"),
                SnoozeEnd.RulesPermit,
                setOf(ruleName("Work")),
            )
    }

    @Test
    fun `rules sharing a name are rejected`() {
        val again = rule("Work", Policy.HoldDuring, lunch, work)
        val other = rule("Lunch", Policy.HoldDuring, lunch, work)

        RuleSet
            .parse(
                listOf(workOnly, other, again),
                ZoneChoice.Device,
            ).shouldBeInstanceOf<RuleSetParse.DuplicateNames>()
            .names shouldBe
            setOf(ruleName("Work"))
    }

    @Test
    fun `names differing only in surrounding space are duplicates`() {
        val padded = rule(" Work ", Policy.HoldDuring, lunch, work)

        RuleSet
            .parse(
                listOf(workOnly, padded),
                ZoneChoice.Device,
            ).shouldBeInstanceOf<RuleSetParse.DuplicateNames>()
            .names shouldBe
            setOf(ruleName("Work"))
    }

    @Test
    fun `parsed rules do not follow later changes to the given list`() {
        val given = mutableListOf(workOnly)
        val parsed = RuleSet.parse(given, ZoneChoice.Device).shouldBeInstanceOf<RuleSetParse.Valid>().rules

        given.clear()

        parsed.decide(post(SLACK_ORIGIN), evening) shouldBe untilMorning
    }

    @Test
    fun `only rules holding when it is posted are named`() {
        val untilEightThirty = rule("B", Policy.HoldDuring, schedule(window(EVERY_DAY, "04:00", "08:30")), work)
        val untilTen = rule("A", Policy.HoldDuring, schedule(window(EVERY_DAY, "04:00", "10:00")), work)
        val eightToNine = rule("C", Policy.HoldDuring, schedule(window(EVERY_DAY, "08:00", "09:00")), work)
        val now = london("2026-10-14T05:00")

        ruleSet(untilEightThirty, untilTen, eightToNine).decide(post(SLACK_ORIGIN), now) shouldBe
            Decision.Snooze(now, london("2026-10-14T10:00"), SnoozeEnd.RulesPermit, setOf(ruleName("B"), ruleName("A")))
    }

    @Test
    fun `a rule that never releases is capped at the ceiling`() {
        val always = rule("Always", Policy.HoldDuring, schedule(window(EVERY_DAY, "00:00", "00:00")), work)

        ruleSet(always).decide(post(SLACK_ORIGIN), evening) shouldBe
            Decision.Snooze(
                evening,
                evening.plus(RuleSet.SNOOZE_CEILING),
                SnoozeEnd.CeilingReached,
                setOf(ruleName("Always")),
            )
    }

    @Test
    fun `a release beyond the ceiling is capped at the ceiling`() {
        val wednesdayNights = rule("Late", Policy.HoldDuring, schedule(window(days(WEDNESDAY), "19:00", "18:00")), work)
        val laterWednesday =
            rule("Wednesday", Policy.DeliverOnlyDuring, schedule(window(days(WEDNESDAY), "20:00", "21:00")), work)
        val now = london("2026-10-14T19:30")

        ruleSet(wednesdayNights, laterWednesday).decide(post(SLACK_ORIGIN), now) shouldBe
            Decision.Snooze(
                now,
                now.plus(RuleSet.SNOOZE_CEILING),
                SnoozeEnd.CeilingReached,
                setOf(ruleName("Late"), ruleName("Wednesday")),
            )
    }

    @Test
    fun `release landing exactly on the ceiling still counts as permitted`() {
        val now = london("2026-10-21T12:00")
        val ceiling = london("2026-10-28T11:00")
        val wednesdayHour =
            rule("Wednesday", Policy.DeliverOnlyDuring, schedule(window(days(WEDNESDAY), "11:00", "12:00")), work)

        now.plus(RuleSet.SNOOZE_CEILING) shouldBe ceiling
        ruleSet(wednesdayHour).decide(post(SLACK_ORIGIN), now) shouldBe
            Decision.Snooze(now, ceiling, SnoozeEnd.RulesPermit, setOf(ruleName("Wednesday")))
    }

    @Test
    fun `snooze duration spans the spring clock change`() {
        val decision = ruleSet(workOnly).decide(post(SLACK_ORIGIN), london("2026-03-27T18:00"))

        decision.shouldBeInstanceOf<Decision.Snooze>().duration shouldBe Duration.ofHours(61)
    }

    @Test
    fun `snooze duration spans the autumn clock change`() {
        val decision = ruleSet(workOnly).decide(post(SLACK_ORIGIN), london("2026-10-23T18:00"))

        decision.shouldBeInstanceOf<Decision.Snooze>().duration shouldBe Duration.ofHours(63)
    }

    private fun post(origin: Origin): PostedNotification = PostedNotification.Snoozable(key, origin)
}
