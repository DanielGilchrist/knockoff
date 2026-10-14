package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test

class RuleTest {
    private val during = london("2026-10-14T12:00")
    private val outside = london("2026-10-14T20:00")
    private val slack = setOf(Target.App(SLACK))
    private val always = schedule(window(EVERY_DAY, "00:00", "00:00"))

    @Test
    fun `disabled rule abstains`() {
        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack, RuleState.Disabled)
            .verdictFor(SLACK_ORIGIN, outside) shouldBe RuleVerdict.Abstains
    }

    @Test
    fun `rule abstains for origins it does not target`() {
        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack).verdictFor(WORK_GMAIL_ORIGIN, outside) shouldBe
            RuleVerdict.Abstains
    }

    @Test
    fun `rule without targets abstains`() {
        rule("r", Policy.HoldDuring, WORK_HOURS, emptySet()).verdictFor(SLACK_ORIGIN, during) shouldBe
            RuleVerdict.Abstains
    }

    @Test
    fun `deliver only during permits inside the schedule`() {
        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, during) shouldBe
            RuleVerdict.Permits
    }

    @Test
    fun `deliver only during holds outside the schedule until it next starts`() {
        rule("r", Policy.DeliverOnlyDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, outside) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-15T08:00")))
    }

    @Test
    fun `deliver only during an always-covered schedule always permits`() {
        rule("r", Policy.DeliverOnlyDuring, always, slack).verdictFor(SLACK_ORIGIN, during) shouldBe RuleVerdict.Permits
    }

    @Test
    fun `hold during holds inside the schedule until it ends`() {
        rule("r", Policy.HoldDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, during) shouldBe
            RuleVerdict.Holds(Release.At(london("2026-10-14T17:00")))
    }

    @Test
    fun `hold during permits outside the schedule`() {
        rule("r", Policy.HoldDuring, WORK_HOURS, slack).verdictFor(SLACK_ORIGIN, outside) shouldBe RuleVerdict.Permits
    }

    @Test
    fun `hold during an always-covered schedule never releases`() {
        rule("r", Policy.HoldDuring, always, slack).verdictFor(SLACK_ORIGIN, during) shouldBe
            RuleVerdict.Holds(Release.Never)
    }
}
