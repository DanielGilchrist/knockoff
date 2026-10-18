package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleState

enum class SetupNotice {
    Clear,
    NoAccess,
    NoNotifications,
    ;

    companion object {
        fun of(
            setup: Setup,
            rules: RuleSet,
        ): SetupNotice {
            val asks = rules.rules.any { it.state == RuleState.Enabled && it.asking.isNotEmpty() }

            return when {
                setup.access == ListenerAccess.NotGranted -> NoAccess
                asks && setup.notifications == NotificationPermission.NotGranted -> NoNotifications
                else -> Clear
            }
        }
    }
}
