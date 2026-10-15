package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.NotificationTraits

sealed interface LeaveReason {
    data class Unsnoozable(
        val traits: NotificationTraits,
    ) : LeaveReason

    data object NoRuleTargets : LeaveReason

    data class Permitted(
        val by: Set<RuleName>,
    ) : LeaveReason
}
