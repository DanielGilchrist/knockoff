package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.EditTarget

data class RuleEditorActions(
    val onDraftChange: (RuleDraft) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
    val onDelete: (EditTarget.Existing) -> Unit,
)
