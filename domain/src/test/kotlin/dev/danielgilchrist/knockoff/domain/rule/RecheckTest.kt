package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.traits
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.ZonedDateTime

class RecheckTest {
    private val key = NotificationKey("0|com.Slack|1|null|10001")
    private val snoozed = PostedNotification.Snoozable(key, SLACK_ORIGIN)
    private val evening = london("2026-10-14T20:00")
    private val slack = setOf(Target.App(SLACK))
    private val work = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack))
    private val earlyWork =
        ruleSet(
            rule(
                "Work",
                Policy.DeliverOnlyDuring,
                schedule(window(days(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "07:00", "17:00")),
                slack,
            ),
        )
    private val disabledWork = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, RuleState.Disabled))
    private val noRules = ruleSet()
    private val takesHolidays = DaysOff(emptyList(), PublicHolidays.Off)
    private val holidayWork =
        ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, daysOff = takesHolidays))
    private val thursday = Holidays(setOf(LocalDate.parse("2026-10-15")))

    @Test
    fun `a hold with the same release is unchanged`() {
        work.recheck(snoozed, before = work, evening) shouldBe Recheck.Unchanged
    }

    @Test
    fun `a hold with an earlier release is resnoozed until it`() {
        earlyWork.recheck(snoozed, before = work, evening) shouldBe
            Recheck.Resnooze(
                Decision.Snooze(evening, london("2026-10-15T07:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work"))),
            )
    }

    @Test
    fun `a new hold is resnoozed`() {
        work.recheck(snoozed, before = noRules, evening) shouldBe
            Recheck.Resnooze(
                Decision.Snooze(evening, london("2026-10-15T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work"))),
            )
    }

    @Test
    fun `a hold whose rule was disabled is released`() {
        disabledWork.recheck(snoozed, before = work, evening) shouldBe
            Recheck.Release(Decision.Leave(LeaveReason.NoRuleTargets))
    }

    @Test
    fun `a hold whose hours now cover the present is released`() {
        earlyWork.recheck(snoozed, before = work, london("2026-10-15T07:30")) shouldBe
            Recheck.Release(Decision.Leave(LeaveReason.Permitted(by = setOf(ruleName("Work")))))
    }

    @Test
    fun `a snooze no rule held before or now is unchanged`() {
        noRules.recheck(snoozed, before = noRules, evening) shouldBe Recheck.Unchanged
    }

    @Test
    fun `a hold with a later release is resnoozed until it`() {
        work.recheck(snoozed, before = earlyWork, evening) shouldBe
            Recheck.Resnooze(
                Decision.Snooze(evening, london("2026-10-15T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work"))),
            )
    }

    @Test
    fun `a hold capped at the ceiling under both rule sets is unchanged`() {
        val always = ruleSet(rule("Always", Policy.HoldDuring, schedule(window(EVERY_DAY, "00:00", "00:00")), slack))
        val alwaysRenamed =
            ruleSet(rule("Forever", Policy.HoldDuring, schedule(window(EVERY_DAY, "00:00", "00:00")), slack))

        alwaysRenamed.recheck(snoozed, before = always, evening) shouldBe Recheck.Unchanged
    }

    @Test
    fun `an unsnoozable notification is unchanged`() {
        val unsnoozable = PostedNotification.Unsnoozable(key, SLACK_ORIGIN, traits(NotificationTrait.Ongoing))

        disabledWork.recheck(unsnoozable, before = work, evening) shouldBe Recheck.Unchanged
    }

    @Test
    fun `describes each outcome`() {
        val snooze =
            Decision.Snooze(evening, london("2026-10-15T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work")))
        val leave = Decision.Leave(LeaveReason.NoRuleTargets)

        Recheck.Unchanged.describe() shouldBe "unchanged"
        Recheck.Resnooze(snooze).describe() shouldBe "re-${snooze.describe()}"
        Recheck.Release(leave).describe() shouldBe "release now, ${leave.describe()}"
    }

    @Test
    fun `a public holiday appearing tomorrow moves the hold to the next working morning`() {
        holidayWork.recheck(snoozed, before = holidayWork, evening, holidays = thursday) shouldBe
            Recheck.Resnooze(
                Decision.Snooze(evening, london("2026-10-16T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work"))),
            )
    }

    @Test
    fun `a public holiday disappearing brings the release back`() {
        holidayWork.recheck(snoozed, before = holidayWork, evening, holidaysBefore = thursday) shouldBe
            Recheck.Resnooze(
                Decision.Snooze(evening, london("2026-10-15T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work"))),
            )
    }

    @Test
    fun `holidays change nothing for a rule that works them`() {
        work.recheck(snoozed, before = work, evening, holidays = thursday) shouldBe Recheck.Unchanged
    }

    @Test
    fun `a public holiday today releases a hold during its hours`() {
        val holdingWork = ruleSet(rule("Focus", Policy.HoldDuring, WORK_HOURS, slack, daysOff = takesHolidays))
        val noon = london("2026-10-15T12:00")

        holdingWork.recheck(snoozed, before = holdingWork, noon, holidays = thursday) shouldBe
            Recheck.Release(Decision.Leave(LeaveReason.Permitted(setOf(ruleName("Focus")))))
    }

    private fun RuleSet.recheck(
        snoozed: PostedNotification,
        before: RuleSet,
        at: ZonedDateTime,
        holidays: Holidays = Holidays.NONE,
        holidaysBefore: Holidays = Holidays.NONE,
    ): Recheck =
        Evaluation(this, at.zone, holidays).recheck(
            snoozed,
            GroupRole.Member,
            Baseline.Known.of(Evaluation(before, at.zone, holidaysBefore)),
            at.toInstant(),
        )
}
