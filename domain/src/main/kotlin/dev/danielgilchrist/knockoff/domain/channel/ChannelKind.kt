package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId

sealed interface ChannelKind {
    data object Shared : ChannelKind

    data class ConversationOf(
        val parent: ChannelId,
        val id: ConversationId,
    ) : ChannelKind
}
