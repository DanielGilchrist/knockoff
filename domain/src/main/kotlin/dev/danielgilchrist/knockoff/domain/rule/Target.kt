package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface Target {
    val packageName: PackageName

    data class App(
        override val packageName: PackageName,
    ) : Target

    data class GmailInbox(
        val address: AccountAddress,
    ) : Target {
        override val packageName: PackageName get() = PackageName.GMAIL
    }

    data class Chat(
        override val packageName: PackageName,
        val channel: ChannelId,
        val id: ConversationId,
        val name: ConversationName,
    ) : Target {
        val key: ChatKey get() = ChatKey(packageName, id)
    }

    fun matches(origin: Origin): Boolean =
        when (this) {
            is App -> {
                origin.packageName == packageName
            }

            is GmailInbox -> {
                when (val channel = origin.channel) {
                    is Channel.Named -> covers(ChannelKey(origin.packageName, channel.id), ChannelKind.Shared)
                    Channel.Unnamed -> false
                }
            }

            is Chat -> {
                origin.packageName == packageName && origin.conversation == Conversation.Known(id)
            }
        }

    fun covers(
        channel: ChannelKey,
        kind: ChannelKind,
    ): Boolean =
        when (this) {
            is App -> channel.packageName == packageName
            is GmailInbox -> channel.packageName == packageName && channel.channelId.endsWithAccount(address)
            is Chat -> channel.packageName == packageName && kind == ChannelKind.ConversationOf(this.channel, id)
        }
}
