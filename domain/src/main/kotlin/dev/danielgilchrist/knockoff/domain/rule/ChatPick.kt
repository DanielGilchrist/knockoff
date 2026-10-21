package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class ChatPick(
    val channel: ChannelId,
    val id: ConversationId,
    val name: ConversationName,
) {
    companion object {
        fun of(chat: Target.Chat): ChatPick = ChatPick(chat.channel, chat.id, chat.name)
    }

    fun inApp(packageName: PackageName): Target.Chat = Target.Chat(packageName, channel, id, name)
}
