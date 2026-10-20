package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
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
                Conversation.None, is Conversation.Routed -> {
                    NotAChat
                }

                is Conversation.Known -> {
                    when (val channel = origin.channel) {
                        is Channel.Named -> {
                            when (val name = nameOf(conversation, shortcutLabels)) {
                                is ConversationTitle.Named -> {
                                    Seen(Target.Chat(origin.packageName, channel.id, conversation.id, name.name))
                                }

                                ConversationTitle.Untitled -> {
                                    NotAChat
                                }
                            }
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
        ): ConversationTitle =
            when (val name = labelOrTitle(conversation, shortcutLabels)) {
                is ConversationTitle.Named -> {
                    when (name.name.value) {
                        conversation.id.value -> ConversationTitle.Untitled
                        else -> name
                    }
                }

                ConversationTitle.Untitled -> {
                    name
                }
            }

        private fun labelOrTitle(
            conversation: Conversation.Known,
            shortcutLabels: List<String>,
        ): ConversationTitle =
            when (val label = ConversationTitle.of(shortcutLabels)) {
                is ConversationTitle.Named -> label
                ConversationTitle.Untitled -> conversation.title
            }
    }
}
