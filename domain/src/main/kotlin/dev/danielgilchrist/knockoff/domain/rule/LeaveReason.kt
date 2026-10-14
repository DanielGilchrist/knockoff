package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait

sealed interface LeaveReason {
    data class Unsnoozable(
        val traits: Set<NotificationTrait>,
    ) : LeaveReason

    data object NoRuleTargets : LeaveReason

    data class Permitted(
        val by: Set<RuleName>,
    ) : LeaveReason
}
