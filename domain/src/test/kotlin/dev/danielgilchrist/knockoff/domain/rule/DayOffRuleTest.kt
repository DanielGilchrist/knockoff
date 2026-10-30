package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.LONDON
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneContext
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class DayOffRuleTest {
    private val local = ZoneContext(LONDON, ZoneChoice.Device)
    private val slack = setOf(Target.App(SLACK))
    private val wednesday = LocalDate.parse("2026-10-14")
    private val noon = london("2026-10-14T12:00")
    private val sick = DaysOff(listOf(Leave.day(wednesday)), PublicHolidays.AsUsual)
    private val holiday = Holidays(setOf(wednesday))

    @Test
    fun `on a day off a rule that lets through during its hours holds until the next working day`() {
        rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, daysOff = sick)
            .verdictFor(SLACK_ORIGIN, noon, local, Holidays.NONE) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-15T08:00")))
    }

    @Test
    fun `on a day off a rule that holds during its hours lets through`() {
        rule("Focus", Policy.HoldDuring, WORK_HOURS, slack, daysOff = sick)
            .verdictFor(SLACK_ORIGIN, noon, local, Holidays.NONE) shouldBe RuleVerdict.Permits
    }

    @Test
    fun `public holidays are days off only for rules that take them off`() {
        val off = DaysOff(emptyList(), PublicHolidays.Off)

        rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, daysOff = off)
            .verdictFor(SLACK_ORIGIN, noon, local, holiday) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-15T08:00")))
        rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack)
            .verdictFor(SLACK_ORIGIN, noon, local, holiday) shouldBe RuleVerdict.Permits
    }

    @Test
    fun `the day off is read in the rule's own zone`() {
        val sydney = RuleZone.Chosen(ZoneChoice.Fixed(ZoneId.of("Australia/Sydney")))
        val work = rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, zone = sydney, daysOff = sick)

        work.dayOffAt(london("2026-10-13T20:00"), local, Holidays.NONE) shouldBe DayOff.OnLeave
        work.dayOffAt(london("2026-10-14T20:00"), local, Holidays.NONE) shouldBe DayOff.Scheduled
        rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, daysOff = DaysOff(emptyList(), PublicHolidays.Off))
            .dayOffAt(noon, local, holiday) shouldBe DayOff.PublicHoliday
    }

    @Test
    fun `a notification on a day off is held until the next working morning`() {
        val work = rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack, daysOff = sick)

        ruleSet(work).decide(
            PostedNotification.Snoozable(NotificationKey("0|com.Slack|1|null|10001"), SLACK_ORIGIN),
            noon,
            Holidays.NONE,
        ) shouldBe Decision.Snooze(noon, london("2026-10-15T08:00"), SnoozeEnd.RulesPermit, setOf(ruleName("Work")))
    }

    @Test
    fun `the next boundary skips a public holiday`() {
        val work =
            rule(
                "Work",
                Policy.DeliverOnlyDuring,
                WORK_HOURS,
                slack,
                daysOff = DaysOff(emptyList(), PublicHolidays.Off),
            )

        ruleSet(work).nextBoundary(london("2026-10-13T17:00"), holiday) shouldBe Boundary.At(london("2026-10-15T08:00"))
        ruleSet(work).nextBoundary(london("2026-10-13T17:00"), Holidays.NONE) shouldBe
            Boundary.At(london("2026-10-14T08:00"))
    }
}
