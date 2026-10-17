package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.Target

data class ChatQuestion(
    val chat: Target.Chat,
    val rules: List<RuleName>,
)
