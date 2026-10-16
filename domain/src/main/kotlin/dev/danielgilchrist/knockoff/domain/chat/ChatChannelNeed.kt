package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface ChatChannelNeed {
    val chat: Target.Chat

    data class Create(
        override val chat: Target.Chat,
    ) : ChatChannelNeed

    data class ParentMissing(
        override val chat: Target.Chat,
    ) : ChatChannelNeed

    data class ParentSilenced(
        override val chat: Target.Chat,
    ) : ChatChannelNeed
}
