package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.ui.RuleSection

data class RuleOverviewActions(
    val onBack: () -> Unit,
    val onStateChange: (RuleState) -> Unit,
    val dayOff: DayOffActions,
    val onEditSection: (RuleSection) -> Unit,
    val onRename: () -> Unit,
    val onDelete: () -> Unit,
)
