package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.ui.editor.DraftProblem
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft

sealed interface Editing {
    data object Closed : Editing

    data class Open(
        val target: EditTarget,
        val draft: RuleDraft,
        val problems: List<DraftProblem>,
    ) : Editing
}
