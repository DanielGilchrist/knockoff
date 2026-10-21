package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.AppFocus
import dev.danielgilchrist.knockoff.ui.Editing

data class WizardActions(
    val onDraftChange: (RuleDraft) -> Unit,
    val onGuide: ((Editing.Guided) -> Editing.Active) -> Unit,
    val onCreate: () -> Unit,
    val onCancel: () -> Unit,
    val onFocus: (AppFocus) -> Unit,
)
