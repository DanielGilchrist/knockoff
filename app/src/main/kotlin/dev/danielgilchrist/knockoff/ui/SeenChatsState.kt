package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface SeenChatsState {
    data object Loading : SeenChatsState

    data class Loaded(
        val chats: List<Target.Chat>,
    ) : SeenChatsState

    data class Failed(
        val detail: String,
    ) : SeenChatsState

    companion object {
        fun of(load: SeenChatsLoad): SeenChatsState =
            when (load) {
                is SeenChatsLoad.Loaded -> Loaded(load.seen.chats)
                is SeenChatsLoad.Corrupt -> Failed(load.detail)
                is SeenChatsLoad.ReadFailed -> Failed(load.detail)
            }
    }
}
