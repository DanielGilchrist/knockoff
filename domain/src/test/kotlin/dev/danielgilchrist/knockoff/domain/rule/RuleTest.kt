package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.LONDON
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.breakNamed
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneContext
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RuleTest {
    private val local = ZoneContext(LONDON, ZoneChoice.Device)
    private val during = london("2026-10-14T12:00")
    private val outside = london("2026-10-14T20:00")
    private val slack = setOf(Target.App(SLACK))
    private val always = schedule(window(EVERY_DAY, "00:00", "00:00"))
    private val lunchBreak =
        schedule(
            window(EVERY_DAY, "08:00", "17:00"),
            breaks = listOf(breakNamed("Lunch", window(EVERY_DAY, "12:00", "13:00"))),
        )

    @Test
    fun `disabled rule abstains`() {
        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack, RuleState.Disabled)
            .verdictFor(SLACK_ORIGIN, outside, local) shouldBe RuleVerdict.Abstains
    }

    @Test
    fun `rule abstains for origins it does not target`() {
        rule(
            "r",
            Policy.DeliverOnlyDuring,
            WORK_HOURS,
            slack,
        ).verdictFor(WORK_GMAIL_ORIGIN, outside, local) shouldBe
            RuleVerdict.Abstains
    }

    @Test
    fun `rule without targets abstains`() {
        rule(
            "r",
            Policy.HoldDuring,
            WORK_HOURS,
            emptySet(),
        ).verdictFor(SLACK_ORIGIN, during, local) shouldBe
            RuleVerdict.Abstains
    }

    @Test
    fun `deliver only during permits inside the schedule`() {
        rule(
            "r",
            Policy.DeliverOnlyDuring,
            WORK_HOURS,
            slack,
        ).verdictFor(SLACK_ORIGIN, during, local) shouldBe
            RuleVerdict.Permits
    }

    @Test
    fun `deliver only during holds outside the schedule until it next starts`() {
        rule(
            "r",
            Policy.DeliverOnlyDuring,
            WORK_HOURS,
            slack,
        ).verdictFor(SLACK_ORIGIN, outside, local) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-15T08:00")))
    }

    @Test
    fun `deliver only during an always-covered schedule always permits`() {
        rule("r", Policy.DeliverOnlyDuring, always, slack).verdictFor(SLACK_ORIGIN, during, local) shouldBe
            RuleVerdict.Permits
    }

    @Test
    fun `hold during holds inside the schedule until it ends`() {
        rule("r", Policy.HoldDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, during, local) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-14T17:00")))
    }

    @Test
    fun `hold during permits outside the schedule`() {
        rule("r", Policy.HoldDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, outside, local) shouldBe
            RuleVerdict.Permits
    }

    @Test
    fun `hold during an always-covered schedule never releases`() {
        rule("r", Policy.HoldDuring, always, slack).verdictFor(SLACK_ORIGIN, during, local) shouldBe
            RuleVerdict.Holds(Release.Never)
    }

    @Test
    fun `a rule's schedule follows its own zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val londonEvening = london("2026-10-14T20:00")
        val untilTokyoMorning =
            RuleVerdict.Holds(
                Release.At(ZonedDateTime.of(LocalDateTime.parse("2026-10-15T08:00"), tokyo)),
            )
        val workHours = rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack)

        workHours
            .copy(
                zone = RuleZone.Chosen(ZoneChoice.Fixed(tokyo)),
            ).verdictFor(SLACK_ORIGIN, londonEvening, local) shouldBe
            untilTokyoMorning
        workHours.verdictFor(SLACK_ORIGIN, londonEvening, ZoneContext(LONDON, ZoneChoice.Fixed(tokyo))) shouldBe
            untilTokyoMorning
    }

    @Test
    fun `a device zone choice overrides a fixed default`() {
        val tokyoDefault = ZoneContext(LONDON, ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo")))

        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack, zone = RuleZone.Chosen(ZoneChoice.Device))
            .verdictFor(SLACK_ORIGIN, london("2026-10-14T20:00"), tokyoDefault) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-15T08:00")))
    }

    @Test
    fun `hold rules and always schedules use the rule's zone`() {
        val tokyo = RuleZone.Chosen(ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo")))
        val tokyoWorkday = london("2026-10-14T02:00")

        rule(
            "r",
            Policy.HoldDuring,
            WORK_HOURS,
            slack,
            zone = tokyo,
        ).verdictFor(SLACK_ORIGIN, tokyoWorkday, local) shouldBe
            RuleVerdict.Holds(
                Release.At(ZonedDateTime.of(LocalDateTime.parse("2026-10-14T17:00"), ZoneId.of("Asia/Tokyo"))),
            )
        rule(
            "r",
            Policy.HoldDuring,
            WORK_HOURS,
            slack,
            zone = tokyo,
        ).verdictFor(SLACK_ORIGIN, london("2026-10-14T20:00"), local) shouldBe
            RuleVerdict.Permits
        rule("r", Policy.HoldDuring, always, slack, zone = tokyo).verdictFor(SLACK_ORIGIN, tokyoWorkday, local) shouldBe
            RuleVerdict.Holds(Release.Never)
    }

    @Test
    fun `a zone whose clocks change on another date keeps its own offset`() {
        val newYork = RuleZone.Chosen(ZoneChoice.Fixed(ZoneId.of("America/New_York")))
        val londonMorningAfterEuropeChanged = london("2026-10-27T11:00")

        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack, zone = newYork)
            .verdictFor(SLACK_ORIGIN, londonMorningAfterEuropeChanged, local) shouldBe
            RuleVerdict.Holds(
                Release.At(ZonedDateTime.of(LocalDateTime.parse("2026-10-27T08:00"), ZoneId.of("America/New_York"))),
            )
    }

    @Test
    fun `deliver only during holds through a break until it ends`() {
        rule(
            "r",
            Policy.DeliverOnlyDuring,
            lunchBreak,
            slack,
        ).verdictFor(SLACK_ORIGIN, london("2026-10-14T12:30"), local) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-14T13:00")))
    }

    @Test
    fun `hold during lets a break through and holds again after it`() {
        val holding = rule("r", Policy.HoldDuring, lunchBreak, slack)

        holding.verdictFor(SLACK_ORIGIN, london("2026-10-14T12:30"), local) shouldBe RuleVerdict.Permits
        holding.verdictFor(SLACK_ORIGIN, london("2026-10-14T11:00"), local) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-14T12:00")))
    }

    @Test
    fun `adding a chat replaces one with the same key and keeps other targets`() {
        val app = Target.App(SLACK)
        val renamed = COLLEAGUE_CHAT.copy(name = ConversationName("Renamed"))
        val other = COLLEAGUE_CHAT.copy(packageName = SLACK)
        val rule = rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(app, COLLEAGUE_CHAT))

        rule.withChat(renamed).targets shouldBe setOf(app, renamed)
        rule.withChat(other).targets shouldBe setOf(app, COLLEAGUE_CHAT, other)
    }
}
