package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface RuleChange {
    fun applyTo(rule: Rule): Rule

    data class SetState(
        val state: RuleState,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.copy(state = state)
    }

    data class AddChat(
        val chat: Target.Chat,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.withChat(chat)
    }
}
