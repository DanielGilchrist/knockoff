package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.settings.Parsed
import dev.danielgilchrist.knockoff.domain.settings.StoredChat
import dev.danielgilchrist.knockoff.domain.settings.parseEach
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSeenChats(
    private val version: Int,
    private val chats: List<StoredChat>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(seen: SeenChats): StoredSeenChats = StoredSeenChats(CURRENT_VERSION, seen.chats.map(StoredChat::from))
    }

    fun toSeenChats(): SeenChatsDecode =
        when (val parsed = chats.parseEach("chat") { it.parse() }) {
            is Parsed.Ok -> SeenChatsDecode.Decoded(SeenChats.of(parsed.value.filter { it.name.value != it.id.value }))
            is Parsed.Failed -> SeenChatsDecode.Invalid(parsed.detail)
        }
}
