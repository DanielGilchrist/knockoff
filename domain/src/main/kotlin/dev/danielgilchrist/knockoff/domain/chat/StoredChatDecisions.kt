package dev.danielgilchrist.knockoff.domain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatDecisions(
    private val version: Int,
    private val letThrough: List<StoredChatKey>,
    private val asked: List<StoredChatKey>,
) : StoredChatDecisionsDocument {
    companion object {
        const val CURRENT_VERSION = 2

        fun from(decisions: ChatDecisions): StoredChatDecisions =
            StoredChatDecisions(
                CURRENT_VERSION,
                decisions.letThrough.map(StoredChatKey::from),
                decisions.asked.map(StoredChatKey::from),
            )
    }

    override fun toDecisions(): ChatDecisions =
        ChatDecisions.of(letThrough.map(StoredChatKey::toKey).toSet(), asked.map(StoredChatKey::toKey).toSet())
}
