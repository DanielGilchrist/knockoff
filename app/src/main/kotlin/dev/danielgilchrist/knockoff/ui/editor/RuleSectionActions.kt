package dev.danielgilchrist.knockoff.ui.editor

data class RuleSectionActions(
    val onDraftChange: (RuleDraft) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
)
