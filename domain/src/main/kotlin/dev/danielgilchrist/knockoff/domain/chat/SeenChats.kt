package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.Target

@ConsistentCopyVisibility
data class SeenChats private constructor(
    val chats: List<Target.Chat>,
) {
    companion object {
        const val LIMIT = 200

        val NONE = SeenChats(emptyList())

        internal fun of(chats: List<Target.Chat>): SeenChats = SeenChats(chats.distinctBy { it.key }.take(LIMIT))

        fun fromJson(text: String): SeenChatsDecode =
            when (
                val decode =
                    VersionedJson.decode(text, mapOf(StoredSeenChats.CURRENT_VERSION to StoredSeenChats.serializer()))
            ) {
                is VersionedDecode.Decoded -> decode.value.toSeenChats()
                is VersionedDecode.Unreadable -> SeenChatsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> SeenChatsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun seen(chat: Target.Chat): SeenChats = of(listOf(chat) + chats)

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSeenChats.from(this))
}
