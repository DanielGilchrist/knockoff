package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.traits
import org.junit.jupiter.api.Test

class DecisionTest {
    @Test
    fun `unsnoozable leave lists its traits`() {
        val traits = traits(NotificationTrait.ForegroundService, NotificationTrait.Ongoing)

        Decision.Leave(LeaveReason.Unsnoozable(traits)).describe() shouldBe
            "leave: unsnoozable (ForegroundService, Ongoing)"
    }

    @Test
    fun `untargeted leave says no rule targets it`() {
        Decision.Leave(LeaveReason.NoRuleTargets).describe() shouldBe "leave: no enabled rule targets it"
    }

    @Test
    fun `permitted leave names the rules`() {
        Decision.Leave(LeaveReason.Permitted(setOf(ruleName("Work"), ruleName("Lunch")))).describe() shouldBe
            "leave: permitted by Work, Lunch"
    }

    @Test
    fun `snooze gives its duration, release, reason and rules`() {
        val snooze =
            Decision.Snooze(
                london("2026-10-14T20:00"),
                london("2026-10-15T08:00"),
                SnoozeEnd.RulesPermit,
                setOf(ruleName("Work"), ruleName("Evenings")),
            )

        snooze.describe() shouldBe
            "snooze for PT12H until 2026-10-15T08:00+01:00[Europe/London] (RulesPermit) held by Work, Evenings"
    }
}
