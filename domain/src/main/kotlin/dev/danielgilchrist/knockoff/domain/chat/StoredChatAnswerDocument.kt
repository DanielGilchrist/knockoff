package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.settings.Parsed
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatAnswerDocument(
    private val version: Int,
    private val answer: StoredChatAnswer,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(answer: ChatAnswer): StoredChatAnswerDocument =
            StoredChatAnswerDocument(CURRENT_VERSION, StoredChatAnswer.from(answer))
    }

    fun toAnswer(): ChatAnswerDecode =
        when (val parsed = answer.parse()) {
            is Parsed.Ok -> ChatAnswerDecode.Decoded(parsed.value)
            is Parsed.Failed -> ChatAnswerDecode.Failed(parsed.detail)
        }
}
