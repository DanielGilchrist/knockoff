package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import java.time.Instant
import java.time.ZoneId

data class Evaluation(
    val rules: RuleSet,
    val zone: ZoneId,
) {
    fun decide(
        notification: PostedNotification,
        at: Instant,
    ): Decision = rules.decide(notification, at.atZone(zone))

    fun recheck(
        snoozed: PostedNotification,
        before: Evaluation,
        at: Instant,
    ): Recheck {
        val previous = before.decide(snoozed, at)

        return when (val current = decide(snoozed, at)) {
            is Decision.Snooze -> {
                when {
                    previous is Decision.Snooze && previous.until.isEqual(current.until) -> Recheck.Unchanged
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
}
