package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.Target

data class ChatQuestion(
    val chat: Target.Chat,
    private val first: RuleName,
    private val others: List<RuleName> = emptyList(),
) {
    val rules: List<RuleName>
        get() = listOf(first) + others
}
