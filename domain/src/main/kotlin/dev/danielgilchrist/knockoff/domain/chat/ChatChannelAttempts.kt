package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.Target

@ConsistentCopyVisibility
data class ChatChannelAttempts private constructor(
    private val refusals: Map<Target.Chat, Int>,
    private val givenUp: Set<Target.Chat>,
    private val missing: Set<Target.Chat>,
) {
    companion object {
        private const val REFUSALS_BEFORE_GIVING_UP = 2

        val NONE: ChatChannelAttempts get() = ChatChannelAttempts(emptyMap(), emptySet(), emptySet())
    }

    fun plan(needs: Set<ChatChannelNeed>): ChatChannelPlan {
        val missingNow = needs.parentsMissing()

        return ChatChannelPlan(
            create = needs.creatable() - givenUp,
            newlyMissing = missingNow - missing,
            waiting = needs.parentsSilenced(),
            next = copy(missing = missingNow),
        )
    }

    fun refused(chat: Target.Chat): ChatChannelAttempts {
        val count = refusals.getOrDefault(chat, 0) + 1

        return when {
            count < REFUSALS_BEFORE_GIVING_UP -> copy(refusals = refusals + (chat to count))
            else -> copy(refusals = refusals - chat, givenUp = givenUp + chat)
        }
    }

    fun unsupported(chat: Target.Chat): ChatChannelAttempts = copy(givenUp = givenUp + chat)

    private fun Set<ChatChannelNeed>.creatable(): Set<Target.Chat> =
        flatMap { need ->
            when (need) {
                is ChatChannelNeed.Create -> listOf(need.chat)
                is ChatChannelNeed.ParentMissing, is ChatChannelNeed.ParentSilenced -> emptyList()
            }
        }.toSet()

    private fun Set<ChatChannelNeed>.parentsMissing(): Set<Target.Chat> =
        flatMap { need ->
            when (need) {
                is ChatChannelNeed.ParentMissing -> listOf(need.chat)
                is ChatChannelNeed.Create, is ChatChannelNeed.ParentSilenced -> emptyList()
            }
        }.toSet()

    private fun Set<ChatChannelNeed>.parentsSilenced(): Set<Target.Chat> =
        flatMap { need ->
            when (need) {
                is ChatChannelNeed.ParentSilenced -> listOf(need.chat)
                is ChatChannelNeed.Create, is ChatChannelNeed.ParentMissing -> emptyList()
            }
        }.toSet()
}
