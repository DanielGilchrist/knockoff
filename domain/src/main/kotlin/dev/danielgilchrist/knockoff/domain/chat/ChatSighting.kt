package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
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
            names: List<String>,
        ): ChatSighting =
            when (notification) {
                is PostedNotification.Snoozable -> of(notification.origin, names)
                is PostedNotification.Unsnoozable -> NotAChat
            }

        private fun of(
            origin: Origin,
            names: List<String>,
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
                                    ConversationName.chosen(names, conversation.id),
                                ),
                            )
                        }

                        Channel.Unnamed -> {
                            NotAChat
                        }
                    }
                }
            }
    }
}
