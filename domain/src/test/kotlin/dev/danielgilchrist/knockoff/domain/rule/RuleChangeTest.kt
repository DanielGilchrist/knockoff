package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class RuleChangeTest {
    private val first = ruleSet(rule("Lunch", Policy.HoldDuring, WORK_HOURS, setOf(Target.App(SLACK))))
    private val second = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(Target.App(SLACK))))
    private val third = ruleSet(rule("Work", Policy.HoldDuring, WORK_HOURS, setOf(Target.App(SLACK))))

    @Test
    fun `the first rules are initial`() {
        RuleChange.Pending.next(first) shouldBe RuleChange.Initial(first)
    }

    @Test
    fun `rules after the initial ones are a change from them`() {
        RuleChange.Initial(first).next(second) shouldBe RuleChange.Changed(before = first, after = second)
    }

    @Test
    fun `rules after a change are a change from its latest rules`() {
        val change =
            RuleChange
                .Changed(
                    before = first,
                    after = second,
                ).next(third)
                .shouldBeInstanceOf<RuleChange.Changed>()

        change.before shouldBe second
        change.after shouldBe third
    }
}
