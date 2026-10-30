package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules

internal data class Applying(
    val rules: AppliedRules,
    val chats: SeenChats,
    val holidays: Holidays,
) {
    val evaluation: Evaluation = rules.evaluation(chats, holidays)
}
