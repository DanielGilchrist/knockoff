package dev.danielgilchrist.knockoff.ui.home

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.SystemActions

data class HomeActions(
    val system: SystemActions,
    val onOpenPreferences: () -> Unit,
    val onNewRule: () -> Unit,
    val onEditRule: (Rule) -> Unit,
    val onRuleChange: (RuleName, RuleChange) -> Unit,
    val onLetChatThrough: (Target.Chat) -> Unit,
    val onResetToDefaults: () -> Unit,
)
