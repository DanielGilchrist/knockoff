package dev.danielgilchrist.knockoff.ask

sealed interface AnswerOutcome {
    data object Saved : AnswerOutcome

    data class Failed(
        val detail: String,
    ) : AnswerOutcome
}
