package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings

internal fun Settings.withRule(
    target: EditTarget,
    rule: Rule,
): RuleSetParse =
    rules.withRules(
        when (target) {
            EditTarget.New -> rules.rules + rule
            is EditTarget.Existing -> rules.rules.map { if (it.name == target.rule.name) rule else it }
        },
    )

internal fun Settings.withoutRule(name: RuleName): RuleSetParse =
    rules.withRules(
        rules.rules.filterNot {
            it.name == name
        },
    )

internal fun Settings.withRuleState(
    name: RuleName,
    state: RuleState,
): RuleSetParse = rules.withRules(rules.rules.map { if (it.name == name) it.copy(state = state) else it })
