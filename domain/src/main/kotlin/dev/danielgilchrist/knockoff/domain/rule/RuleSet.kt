package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import java.time.Duration
import java.time.ZonedDateTime

@JvmInline
value class RuleSet private constructor(
    val rules: List<Rule>,
) {
    companion object {
        val SNOOZE_CEILING: Duration = Duration.ofDays(7)

        internal fun of(rule: Rule): RuleSet = RuleSet(listOf(rule))

        fun parse(rules: List<Rule>): RuleSetParse {
            val duplicates =
                rules
                    .groupingBy { it.name }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys

            return when {
                duplicates.isEmpty() -> RuleSetParse.Valid(RuleSet(rules.toList()))
                else -> RuleSetParse.DuplicateNames(duplicates)
            }
        }
    }

    fun decide(
        notification: PostedNotification,
        now: ZonedDateTime,
    ): Decision =
        when (notification) {
            is PostedNotification.Unsnoozable -> Decision.Leave(LeaveReason.Unsnoozable(notification.traits))
            is PostedNotification.Snoozable -> decide(notification.origin, now)
        }

    fun recheck(
        snoozed: PostedNotification,
        before: RuleSet,
        now: ZonedDateTime,
    ): Recheck {
        val previous = before.decide(snoozed, now)

        return when (val current = decide(snoozed, now)) {
            is Decision.Snooze -> {
                when {
                    previous is Decision.Snooze && previous.until == current.until -> Recheck.Unchanged
                    else -> Recheck.Resnooze(current)
                }
            }

            is Decision.Leave -> {
                when (previous) {
                    is Decision.Snooze -> Recheck.Release(current)
                    is Decision.Leave -> Recheck.Unchanged
                }
            }
        }
    }

    private fun decide(
        origin: Origin,
        now: ZonedDateTime,
    ): Decision {
        val holding = holdsAt(origin, now)
        val permitting = rules.filter { it.verdictFor(origin, now) == RuleVerdict.Permits }.map { it.name }.toSet()

        return when {
            holding.isNotEmpty() -> releaseFrom(origin, now, at = now, heldBy = holding.map { it.rule }.toSet())
            permitting.isEmpty() -> Decision.Leave(LeaveReason.NoRuleTargets)
            else -> Decision.Leave(LeaveReason.Permitted(by = permitting))
        }
    }

    private tailrec fun releaseFrom(
        origin: Origin,
        now: ZonedDateTime,
        at: ZonedDateTime,
        heldBy: Set<RuleName>,
    ): Decision.Snooze {
        val ceiling = now.plus(SNOOZE_CEILING)

        return when (val release = holdsAt(origin, at).firstOrNull()?.release) {
            null -> {
                Decision.Snooze(now, at, SnoozeEnd.RulesPermit, heldBy)
            }

            Release.Never -> {
                Decision.Snooze(now, ceiling, SnoozeEnd.CeilingReached, heldBy)
            }

            is Release.At -> {
                when {
                    release.time.isAfter(ceiling) -> Decision.Snooze(now, ceiling, SnoozeEnd.CeilingReached, heldBy)
                    else -> releaseFrom(origin, now, release.time, heldBy)
                }
            }
        }
    }

    private fun holdsAt(
        origin: Origin,
        at: ZonedDateTime,
    ): List<Hold> =
        rules.flatMap { rule ->
            when (val verdict = rule.verdictFor(origin, at)) {
                is RuleVerdict.Holds -> listOf(Hold(rule.name, verdict.release))
                RuleVerdict.Abstains, RuleVerdict.Permits -> emptyList()
            }
        }
}
