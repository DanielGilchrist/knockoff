package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions

sealed interface ChatDecisionsLoad {
    data class Loaded(
        val decisions: ChatDecisions,
    ) : ChatDecisionsLoad

    data class Corrupt(
        val detail: String,
    ) : ChatDecisionsLoad

    data class ReadFailed(
        val detail: String,
    ) : ChatDecisionsLoad
}
