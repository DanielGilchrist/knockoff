package dev.danielgilchrist.knockoff.domain.notification

sealed interface Conversation {
    data class Known(
        val id: ConversationId,
        val title: ConversationTitle,
    ) : Conversation

    data object None : Conversation
}
