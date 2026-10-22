package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules

internal data class Applying(
    val rules: AppliedRules,
    val chats: SeenChats,
) {
    val evaluation: Evaluation = rules.evaluation(chats)
}
