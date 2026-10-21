package dev.danielgilchrist.knockoff.ui.editor

data class AppScreenActions(
    val onDraftChange: (AppDraft) -> Unit,
    val onRemove: () -> Unit,
    val onBack: () -> Unit,
)
