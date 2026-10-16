package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChat(
    private val app: String,
    private val channel: String,
    private val id: String,
    private val name: String,
) {
    companion object {
        fun from(chat: Target.Chat): StoredChat =
            StoredChat(chat.packageName.value, chat.channel.value, chat.id.value, chat.name.value)
    }

    fun parse(): Parsed<Target.Chat> =
        when {
            app.isBlank() -> {
                Parsed.Failed("blank chat app")
            }

            channel.isBlank() -> {
                Parsed.Failed("blank chat channel")
            }

            id.isBlank() -> {
                Parsed.Failed("blank chat id")
            }

            else -> {
                Parsed.Ok(
                    Target.Chat(PackageName(app), ChannelId(channel), ConversationId(id), ConversationName(name)),
                )
            }
        }
}
