package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import java.time.LocalDate

sealed interface RuleChange {
    fun applyTo(rule: Rule): Rule

    data class SetState(
        val state: RuleState,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.copy(state = state)
    }

    data class TakeOff(
        val date: LocalDate,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.copy(daysOff = rule.daysOff.taking(Leave.day(date), date))
    }

    data class BackOn(
        val date: LocalDate,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.copy(daysOff = rule.daysOff.backOn(date))
    }

    data class AddChat(
        val chat: Target.Chat,
    ) : RuleChange {
        override fun applyTo(rule: Rule): Rule = rule.withChat(chat)
    }
}
