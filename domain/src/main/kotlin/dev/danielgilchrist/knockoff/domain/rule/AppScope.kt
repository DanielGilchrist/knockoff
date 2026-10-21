package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId

sealed interface AppScope {
    data object Everything : AppScope

    @ConsistentCopyVisibility
    data class Narrowed private constructor(
        val accounts: Map<ChannelGroupId, GroupName>,
        val chats: Map<ConversationId, ChatPick>,
        val patterns: Set<PatternPick>,
        val asking: NewChatsAsking,
    ) : AppScope {
        companion object {
            internal fun of(chat: ChatPick): Narrowed =
                Narrowed(emptyMap(), mapOf(chat.id to chat), emptySet(), NewChatsAsking.DontAsk)

            internal fun chosen(
                accounts: Map<ChannelGroupId, GroupName>,
                chats: Collection<ChatPick>,
                patterns: Set<PatternPick>,
                asking: NewChatsAsking,
            ): Narrowed = Narrowed(accounts, chats.associateBy { it.id }, patterns, asking)

            fun parse(
                accounts: Map<ChannelGroupId, GroupName>,
                chats: Collection<ChatPick>,
                patterns: Set<PatternPick>,
                asking: NewChatsAsking,
            ): NarrowedParse =
                when {
                    accounts.isEmpty() && chats.isEmpty() && patterns.isEmpty() && asking == NewChatsAsking.DontAsk -> {
                        NarrowedParse.NothingChosen
                    }

                    else -> {
                        NarrowedParse.Valid(chosen(accounts, chats, patterns, asking))
                    }
                }
        }

        internal fun withChat(chat: ChatPick): Narrowed = copy(chats = chats + (chat.id to chat))

        internal fun withChats(more: Map<ConversationId, ChatPick>): Narrowed = copy(chats = chats + more)
    }
}
