package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredChatKey(
    private val app: String,
    private val id: String,
) {
    companion object {
        fun from(key: ChatKey): StoredChatKey = StoredChatKey(key.packageName.value, key.id.value)
    }

    fun toKey(): ChatKey = ChatKey(PackageName(app), ConversationId(id))
}
