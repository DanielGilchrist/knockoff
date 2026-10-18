package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings

internal fun Settings.withoutRule(name: RuleName): RuleSetParse =
    rules.withRules(
        rules.rules.filterNot {
            it.name == name
        },
    )

internal fun Settings.withRuleChanged(
    name: RuleName,
    change: RuleChange,
): RuleSetParse = rules.withRules(rules.rules.map { if (it.name == name) change.applyTo(it) else it })

internal fun Settings.withSaved(
    editing: Editing.Active,
    edited: Rule,
): RuleSetParse =
    when (editing) {
        is Editing.Guided -> {
            rules.withRules(rules.rules + edited)
        }

        is Editing.Section -> {
            val original = editing.rule.name

            rules.withRules(rules.rules.map { if (it.name == original) editing.section.merge(it, edited) else it })
        }
    }
