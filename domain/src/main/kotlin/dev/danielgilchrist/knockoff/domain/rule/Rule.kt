package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.schedule.Coverage
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import java.time.ZonedDateTime

data class Rule(
    val name: RuleName,
    private val state: RuleState,
    private val policy: Policy,
    private val schedule: Schedule,
    private val targets: Set<Target>,
) {
    fun verdictFor(
        origin: Origin,
        at: ZonedDateTime,
    ): RuleVerdict =
        when (state) {
            RuleState.Disabled -> {
                RuleVerdict.Abstains
            }

            RuleState.Enabled -> {
                when {
                    targets.none { it.matches(origin) } -> RuleVerdict.Abstains
                    else -> scheduledVerdictAt(at)
                }
            }
        }

    private fun scheduledVerdictAt(at: ZonedDateTime): RuleVerdict =
        when (val coverage = schedule.coverageAt(at)) {
            is Coverage.Inside -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Permits
                    Policy.HoldDuring -> RuleVerdict.Holds(Release.At(coverage.endsAt))
                }
            }

            is Coverage.Outside -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Holds(Release.At(coverage.startsAt))
                    Policy.HoldDuring -> RuleVerdict.Permits
                }
            }

            Coverage.Always -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Permits
                    Policy.HoldDuring -> RuleVerdict.Holds(Release.Never)
                }
            }
        }
}
