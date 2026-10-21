package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.AppFocus

data class RuleSectionActions(
    val onDraftChange: (RuleDraft) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
    val onFocus: (AppFocus) -> Unit,
)
