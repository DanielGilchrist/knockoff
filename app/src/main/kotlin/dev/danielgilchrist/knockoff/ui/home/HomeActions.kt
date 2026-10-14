package dev.danielgilchrist.knockoff.ui.home

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleState

data class HomeActions(
    val onOpenAccessSettings: () -> Unit,
    val onNewRule: () -> Unit,
    val onEditRule: (Rule) -> Unit,
    val onRuleStateChange: (RuleName, RuleState) -> Unit,
    val onResetToDefaults: () -> Unit,
)
