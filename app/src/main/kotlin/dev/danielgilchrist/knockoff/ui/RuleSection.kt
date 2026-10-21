package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
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
            What -> {
                current.copy(apps = RuleApps.fromTargets(current.patterns() + edited.picks(), edited.asking))
            }

            When -> {
                current.copy(policy = edited.policy, schedule = edited.schedule)
            }

            Advanced -> {
                current.copy(
                    zone = edited.zone,
                    apps = RuleApps.fromTargets(current.picks() + edited.patterns(), current.asking),
                )
            }
        }

    private fun Rule.patterns(): Set<Target> = targets.filter { it.isPattern() }.toSet()

    private fun Rule.picks(): Set<Target> = targets.filterNot { it.isPattern() }.toSet()

    private fun Target.isPattern(): Boolean =
        when (this) {
            is Target.Pattern -> true
            is Target.App, is Target.Account, is Target.Chat -> false
        }
}
