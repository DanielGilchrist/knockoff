package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.Target

enum class RuleSection {
    What,
    When,
    Advanced,
    ;

    fun merge(
        current: Rule,
        edited: Rule,
    ): Rule =
        when (this) {
            What -> current.copy(targets = current.patterns() + edited.picks(), asking = edited.asking)
            When -> current.copy(policy = edited.policy, schedule = edited.schedule)
            Advanced -> current.copy(zone = edited.zone, targets = current.picks() + edited.patterns())
        }

    private fun Rule.patterns(): Set<Target> = targets.filter { it.isPattern() }.toSet()

    private fun Rule.picks(): Set<Target> = targets.filterNot { it.isPattern() }.toSet()

    private fun Target.isPattern(): Boolean =
        when (this) {
            is Target.Pattern -> true
            is Target.App, is Target.GmailInbox, is Target.Chat -> false
        }
}
