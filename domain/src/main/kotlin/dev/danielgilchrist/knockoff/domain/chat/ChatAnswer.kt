package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface ChatAnswer {
    data class Hold(
        val rule: RuleName,
        val chat: Target.Chat,
    ) : ChatAnswer

    data class LetThrough(
        val chat: ChatKey,
    ) : ChatAnswer

    data class Dismiss(
        val chats: Set<ChatKey>,
    ) : ChatAnswer

    companion object {
        fun fromJson(text: String): ChatAnswerDecode =
            when (
                val decode =
                    VersionedJson.decode(
                        text,
                        mapOf(StoredChatAnswerDocument.CURRENT_VERSION to StoredChatAnswerDocument.serializer()),
                    )
            ) {
                is VersionedDecode.Decoded -> {
                    decode.value.toAnswer()
                }

                is VersionedDecode.Unreadable -> {
                    ChatAnswerDecode.Failed(decode.detail)
                }

                is VersionedDecode.UnsupportedVersion -> {
                    ChatAnswerDecode.Failed(
                        "Unsupported chat answer version ${decode.version}",
                    )
                }
            }
    }

    fun toJson(): String = VersionedJson.json.encodeToString(StoredChatAnswerDocument.from(this))
}
