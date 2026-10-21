package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test

class BoundaryTest {
    private val slack = setOf(Target.App(SLACK))
    private val work = rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slack)
    private val lunch = rule("Lunch", Policy.HoldDuring, schedule(window(EVERY_DAY, "12:00", "13:00")), slack)

    @Test
    fun `outside a window the next boundary is its start`() {
        ruleSet(work).nextBoundary(london("2026-10-14T20:00")) shouldBe Boundary.At(london("2026-10-15T08:00"))
    }

    @Test
    fun `inside a window the next boundary is its end`() {
        ruleSet(work).nextBoundary(london("2026-10-14T10:00")) shouldBe Boundary.At(london("2026-10-14T17:00"))
    }

    @Test
    fun `the earliest boundary of any rule wins`() {
        ruleSet(work, lunch).nextBoundary(london("2026-10-14T10:00")) shouldBe Boundary.At(london("2026-10-14T12:00"))
    }

    @Test
    fun `disabled rules, untargeted rules and always-covered schedules have no boundary`() {
        val always = rule("Always", Policy.HoldDuring, schedule(window(EVERY_DAY, "00:00", "00:00")), slack)
        val disabled = lunch.copy(state = RuleState.Disabled)
        val untargeted = lunch.copy(name = ruleName("Untargeted"), apps = RuleApps.NONE)

        ruleSet(always, disabled, untargeted).nextBoundary(london("2026-10-14T10:00")) shouldBe Boundary.None
    }

    @Test
    fun `a boundary is never now`() {
        ruleSet(work).nextBoundary(london("2026-10-14T17:00")) shouldBe Boundary.At(london("2026-10-15T08:00"))
        ruleSet(work).nextBoundary(london("2026-10-15T08:00")) shouldBe Boundary.At(london("2026-10-15T17:00"))
    }

    @Test
    fun `a boundary no later than a time is the earlier of the two`() {
        val noon = london("2026-10-14T12:00")
        val one = london("2026-10-14T13:00")

        Boundary.At(one).noLaterThan(noon) shouldBe Boundary.At(noon)
        Boundary.At(noon).noLaterThan(one) shouldBe Boundary.At(noon)
        Boundary.None.noLaterThan(noon) shouldBe Boundary.At(noon)
    }
}
