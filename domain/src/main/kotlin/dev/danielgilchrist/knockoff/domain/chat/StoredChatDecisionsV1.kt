package dev.danielgilchrist.knockoff.domain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatDecisionsV1(
    private val version: Int,
    private val letThrough: List<StoredChatKey>,
) : StoredChatDecisionsDocument {
    companion object {
        const val VERSION = 1
    }

    override fun toDecisions(): ChatDecisions =
        StoredChatDecisions(StoredChatDecisions.CURRENT_VERSION, letThrough, emptyList()).toDecisions()
}
