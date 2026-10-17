package dev.danielgilchrist.knockoff.domain.chat

sealed interface ChatAnswerDecode {
    data class Decoded(
        val answer: ChatAnswer,
    ) : ChatAnswerDecode

    data class Failed(
        val detail: String,
    ) : ChatAnswerDecode
}
