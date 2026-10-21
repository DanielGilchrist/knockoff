package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule

enum class RuleSection {
    Apps,
    Hours,
    ;

    fun merge(
        opened: Rule,
        current: Rule,
        edited: Rule,
    ): Rule =
        when (this) {
            Apps -> current.copy(apps = edited.apps.rebased(opened.apps, current.apps))
            Hours -> current.copy(policy = edited.policy, schedule = edited.schedule, zone = edited.zone)
        }
}
