package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.domain.chat.SeenChats

sealed interface SeenChatsLoad {
    data class Loaded(
        val seen: SeenChats,
    ) : SeenChatsLoad

    data class Corrupt(
        val detail: String,
    ) : SeenChatsLoad

    data class ReadFailed(
        val detail: String,
    ) : SeenChatsLoad
}
