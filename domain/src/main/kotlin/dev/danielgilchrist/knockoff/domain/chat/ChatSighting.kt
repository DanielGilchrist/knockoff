package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.ConversationTitle
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface ChatSighting {
    data class Seen(
        val chat: Target.Chat,
    ) : ChatSighting

    data object NotAChat : ChatSighting

    companion object {
        fun of(
            notification: PostedNotification,
            shortcutLabels: List<String>,
        ): ChatSighting =
            when (notification) {
                is PostedNotification.Snoozable -> of(notification.origin, shortcutLabels)
                is PostedNotification.Unsnoozable -> NotAChat
            }

        private fun of(
            origin: Origin,
            shortcutLabels: List<String>,
        ): ChatSighting =
            when (val conversation = origin.conversation) {
                Conversation.None -> {
                    NotAChat
                }

                is Conversation.Known -> {
                    when (val channel = origin.channel) {
                        is Channel.Named -> {
                            Seen(
                                Target.Chat(
                                    origin.packageName,
                                    channel.id,
                                    conversation.id,
                                    nameOf(conversation, shortcutLabels),
                                ),
                            )
                        }

                        Channel.Unnamed -> {
                            NotAChat
                        }
                    }
                }
            }

        private fun nameOf(
            conversation: Conversation.Known,
            shortcutLabels: List<String>,
        ): ConversationName =
            when (val label = ConversationTitle.of(shortcutLabels)) {
                is ConversationTitle.Named -> label.name
                ConversationTitle.Untitled -> titleOrId(conversation)
            }

        private fun titleOrId(conversation: Conversation.Known): ConversationName =
            when (val title = conversation.title) {
                is ConversationTitle.Named -> title.name
                ConversationTitle.Untitled -> ConversationName(conversation.id.value)
            }
    }
}
