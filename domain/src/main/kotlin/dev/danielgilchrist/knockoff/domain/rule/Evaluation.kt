package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import java.time.Instant
import java.time.ZoneId

data class Evaluation(
    val rules: RuleSet,
    val zone: ZoneId,
) {
    fun decide(
        notification: PostedNotification,
        role: GroupRole,
        at: Instant,
    ): Decision =
        when (val own = decide(notification, at)) {
            is Decision.Leave -> {
                own
            }

            is Decision.Snooze -> {
                when (role) {
                    GroupRole.Member -> own
                    is GroupRole.Summary -> held(own, role, at)
                }
            }
        }

    fun recheck(
        snoozed: PostedNotification,
        role: GroupRole,
        before: Baseline.Known<Evaluation>,
        at: Instant,
    ): Recheck {
        val previous = before.candidates.map { it.decide(snoozed, role, at) }

        return when (val current = decide(snoozed, role, at)) {
            is Decision.Snooze -> {
                when {
                    previous.all { it is Decision.Snooze && it.until.isEqual(current.until) } -> Recheck.Unchanged
                    else -> Recheck.Resnooze(current)
                }
            }

            is Decision.Leave -> {
                when {
                    previous.any { it is Decision.Snooze } -> Recheck.Release(current)
                    else -> Recheck.Unchanged
                }
            }
        }
    }

    private fun decide(
        notification: PostedNotification,
        at: Instant,
    ): Decision = rules.decide(notification, at.atZone(zone))

    private fun held(
        own: Decision.Snooze,
        summary: GroupRole.Summary,
        at: Instant,
    ): Decision =
        when {
            summary.showing.any { decide(it, at) is Decision.Leave } -> Decision.Leave(LeaveReason.GroupShowing)
            else -> own
        }
}
