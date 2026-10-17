package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.ChatKey

@ConsistentCopyVisibility
data class ChatDecisions private constructor(
    val letThrough: Set<ChatKey>,
    val asked: Set<ChatKey>,
) {
    companion object {
        val NONE: ChatDecisions get() = ChatDecisions(emptySet(), emptySet())

        internal fun of(
            letThrough: Set<ChatKey>,
            asked: Set<ChatKey>,
        ): ChatDecisions = ChatDecisions(letThrough, asked)

        fun fromJson(text: String): ChatDecisionsDecode =
            when (
                val decode =
                    VersionedJson.decode<StoredChatDecisionsDocument>(
                        text,
                        mapOf(
                            StoredChatDecisionsV1.VERSION to StoredChatDecisionsV1.serializer(),
                            StoredChatDecisions.CURRENT_VERSION to StoredChatDecisions.serializer(),
                        ),
                    )
            ) {
                is VersionedDecode.Decoded -> ChatDecisionsDecode.Decoded(decode.value.toDecisions())
                is VersionedDecode.Unreadable -> ChatDecisionsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> ChatDecisionsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun lettingThrough(key: ChatKey): ChatDecisions = copy(letThrough = letThrough + key)

    fun askedAbout(keys: Set<ChatKey>): ChatDecisions = copy(asked = asked + keys)

    fun toJson(): String = VersionedJson.json.encodeToString(StoredChatDecisions.from(this))
}
