package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber

object AnswerLabels {
    const val LET_THROUGH = "Always let through"
    const val ADD_TO_ANY = "Add to…"

    fun addTo(rule: RuleName): String = "Add to ${rule.value}"

    fun addTo(rule: Rule): String =
        when (rule.state) {
            RuleState.Enabled -> addTo(rule.name)
            RuleState.Disabled -> "${addTo(rule.name)} (off)"
        }

    fun addOrLetThrough(number: GrammaticalNumber): String =
        when (number) {
            GrammaticalNumber.One -> "Add it to a rule, or always let it through"
            GrammaticalNumber.Many -> "Add them to a rule, or always let them through"
        }

    fun choosing(chat: ConversationName): String = "Add ${chat.value} to…"
}
