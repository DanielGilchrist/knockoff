package dev.danielgilchrist.knockoff.domain.chat

internal sealed interface StoredChatDecisionsDocument {
    fun toDecisions(): ChatDecisions
}
