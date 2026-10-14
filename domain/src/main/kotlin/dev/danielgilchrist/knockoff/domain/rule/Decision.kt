package dev.danielgilchrist.knockoff.domain.rule

import java.time.Duration
import java.time.ZonedDateTime

sealed interface Decision {
    data class Leave(
        val reason: LeaveReason,
    ) : Decision

    data class Snooze(
        val from: ZonedDateTime,
        val until: ZonedDateTime,
        val end: SnoozeEnd,
        val heldBy: Set<RuleName>,
    ) : Decision {
        val duration: Duration get() = Duration.between(from, until)
    }

    fun describe(): String =
        when (this) {
            is Leave -> {
                when (reason) {
                    is LeaveReason.Unsnoozable -> "leave: unsnoozable (${reason.traits.joinToString()})"
                    LeaveReason.NoRuleTargets -> "leave: no enabled rule targets it"
                    is LeaveReason.Permitted -> "leave: permitted by ${reason.by.joinToString { it.value }}"
                }
            }

            is Snooze -> {
                "snooze for $duration until $until ($end) held by ${heldBy.joinToString { it.value }}"
            }
        }
}
