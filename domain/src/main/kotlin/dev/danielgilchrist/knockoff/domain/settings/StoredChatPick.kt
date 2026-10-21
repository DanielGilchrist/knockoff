package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatPick(
    private val channel: String,
    private val id: String,
    private val name: String,
) {
    companion object {
        fun from(chat: ChatPick): StoredChatPick = StoredChatPick(chat.channel.value, chat.id.value, chat.name.value)
    }

    fun parse(): Parsed<ChatPick> =
        when {
            channel.isBlank() -> Parsed.Failed("blank chat channel")
            id.isBlank() -> Parsed.Failed("blank chat id")
            name.isBlank() -> Parsed.Failed("blank chat name")
            else -> Parsed.Ok(ChatPick(ChannelId(channel), ConversationId(id), ConversationName(name)))
        }
}
