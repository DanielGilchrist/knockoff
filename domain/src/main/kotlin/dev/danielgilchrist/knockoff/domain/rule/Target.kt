package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.ConversationTitle
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface Target {
    val packageName: PackageName

    fun matches(origin: Origin): Boolean

    fun covers(
        channel: ChannelKey,
        kind: ChannelKind,
    ): Boolean

    fun expandsTo(chat: Chat): Boolean = false

    data class App(
        override val packageName: PackageName,
    ) : Target {
        override fun matches(origin: Origin): Boolean = origin.packageName == packageName

        override fun covers(
            channel: ChannelKey,
            kind: ChannelKind,
        ): Boolean = channel.packageName == packageName
    }

    data class GmailInbox(
        val address: AccountAddress,
    ) : Target {
        override val packageName: PackageName get() = PackageName.GMAIL

        override fun matches(origin: Origin): Boolean =
            when (val channel = origin.channel) {
                is Channel.Named -> covers(ChannelKey(origin.packageName, channel.id), ChannelKind.Shared)
                Channel.Unnamed -> false
            }

        override fun covers(
            channel: ChannelKey,
            kind: ChannelKind,
        ): Boolean = channel.packageName == packageName && channel.channelId.endsWithAccount(address)
    }

    data class Chat(
        override val packageName: PackageName,
        val channel: ChannelId,
        val id: ConversationId,
        val name: ConversationName,
    ) : Target {
        val key: ChatKey get() = ChatKey(packageName, id)

        override fun matches(origin: Origin): Boolean =
            origin.packageName == packageName &&
                when (val conversation = origin.conversation) {
                    is Conversation.Known -> conversation.id == id
                    Conversation.None -> false
                }

        override fun covers(
            channel: ChannelKey,
            kind: ChannelKind,
        ): Boolean = channel.packageName == packageName && kind == ChannelKind.ConversationOf(this.channel, id)
    }

    sealed interface Pattern : Target {
        val pattern: TextPattern
    }

    data class ChannelPattern(
        override val packageName: PackageName,
        override val pattern: TextPattern,
    ) : Pattern {
        override fun matches(origin: Origin): Boolean =
            when (val channel = origin.channel) {
                is Channel.Named -> covers(ChannelKey(origin.packageName, channel.id), ChannelKind.Shared)
                Channel.Unnamed -> false
            }

        override fun covers(
            channel: ChannelKey,
            kind: ChannelKind,
        ): Boolean =
            channel.packageName == packageName &&
                when (kind) {
                    ChannelKind.Shared -> pattern.matches(channel.channelId.value)
                    is ChannelKind.ConversationOf -> pattern.matches(kind.parent.value)
                }
    }

    data class ChatNamePattern(
        override val packageName: PackageName,
        override val pattern: TextPattern,
    ) : Pattern {
        override fun matches(origin: Origin): Boolean =
            origin.packageName == packageName &&
                when (val conversation = origin.conversation) {
                    is Conversation.Known -> {
                        when (val title = conversation.title) {
                            is ConversationTitle.Named -> pattern.matches(title.name.value)
                            ConversationTitle.Untitled -> false
                        }
                    }

                    Conversation.None -> {
                        false
                    }
                }

        override fun covers(
            channel: ChannelKey,
            kind: ChannelKind,
        ): Boolean = false

        override fun expandsTo(chat: Chat): Boolean =
            chat.packageName == packageName && pattern.matches(chat.name.value)
    }
}
