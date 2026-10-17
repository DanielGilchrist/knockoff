package dev.danielgilchrist.knockoff.domain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatDecisions(
    private val version: Int,
    private val letThrough: List<StoredChatKey>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(decisions: ChatDecisions): StoredChatDecisions =
            StoredChatDecisions(CURRENT_VERSION, decisions.letThrough.map(StoredChatKey::from))
    }

    fun toDecisions(): ChatDecisions = ChatDecisions.of(letThrough.map(StoredChatKey::toKey).toSet())
}
