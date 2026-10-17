package dev.danielgilchrist.knockoff.chat

sealed interface ChatDecisionSave {
    data object Saved : ChatDecisionSave

    data class Failed(
        val detail: String,
    ) : ChatDecisionSave
}
