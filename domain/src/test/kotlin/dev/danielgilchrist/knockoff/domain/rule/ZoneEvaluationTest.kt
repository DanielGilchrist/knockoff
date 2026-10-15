package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.ZoneId

class ZoneEvaluationTest {
    private val tokyo = ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo"))
    private val seoul = ZoneChoice.Fixed(ZoneId.of("Asia/Seoul"))
    private val slack = setOf(Target.App(SLACK))
    private val snoozable = PostedNotification.Snoozable(NotificationKey("0|com.Slack|1|null|10001"), SLACK_ORIGIN)
    private val tokyoAfternoonHold =
        rule(
            "Tokyo afternoon",
            Policy.HoldDuring,
            schedule(window(days(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "09:00", "17:00")),
            slack,
            zone = RuleZone.Chosen(tokyo),
        )
    private val localWork = rule("Local work", Policy.DeliverOnlyDuring, WORK_HOURS, slack)

    @Test
    fun `the release search keeps device-zone rules in the device zone`() {
        val decision = ruleSet(tokyoAfternoonHold, localWork).decide(snoozable, london("2026-10-14T08:30"))

        decision.shouldBeInstanceOf<Decision.Snooze>().until shouldBe london("2026-10-14T09:00")
    }

    @Test
    fun `release times are given in the device zone`() {
        ruleSet(
            tokyoAfternoonHold,
        ).decide(snoozable, london("2026-10-14T02:00")).shouldBeInstanceOf<Decision.Snooze>().until shouldBe
            london("2026-10-14T09:00")
    }

    @Test
    fun `the default zone applies to rules that follow it`() {
        val decision = ruleSet(localWork).withDefaultZone(tokyo).decide(snoozable, london("2026-10-14T20:00"))

        decision.shouldBeInstanceOf<Decision.Snooze>().until shouldBe london("2026-10-15T00:00")
    }

    @Test
    fun `the next boundary is the earliest instant across zones, in the device zone`() {
        ruleSet(tokyoAfternoonHold, localWork).nextBoundary(london("2026-10-14T07:00")) shouldBe
            Boundary.At(london("2026-10-14T08:00"))
        ruleSet(tokyoAfternoonHold, localWork).nextBoundary(london("2026-10-14T00:30")) shouldBe
            Boundary.At(london("2026-10-14T01:00"))
    }

    @Test
    fun `moving a hold to another zone with the same offset is unchanged`() {
        val inSeoul = ruleSet(tokyoAfternoonHold.copy(zone = RuleZone.Chosen(seoul)))

        inSeoul.recheck(snoozable, before = ruleSet(tokyoAfternoonHold), london("2026-10-14T02:00")) shouldBe
            Recheck.Unchanged
    }

    @Test
    fun `channels are planned in each rule's zone`() {
        val messages = ChannelKey(SLACK, ChannelId("messages"))
        val read = mapOf(messages to Importance.Default)

        ruleSet(
            tokyoAfternoonHold,
        ).planChannels(read, SilencedChannels.NONE, london("2026-10-14T02:00")).silence shouldBe
            listOf(ChannelChange(messages, Importance.Default, Importance.Low))
        ruleSet(
            tokyoAfternoonHold,
        ).planChannels(read, SilencedChannels.NONE, london("2026-10-14T12:00")).silence shouldBe
            emptyList()
    }
}
