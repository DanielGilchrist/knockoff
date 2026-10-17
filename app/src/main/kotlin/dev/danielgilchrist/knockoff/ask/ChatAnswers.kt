package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.chat.ChatDecisionSave
import dev.danielgilchrist.knockoff.chat.ChatDecisionsStore
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.settings.SettingsUpdate
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.withRuleChanged

class ChatAnswers(
    private val settings: SettingsStore,
    private val decisions: ChatDecisionsStore,
) {
    suspend fun answer(answer: ChatAnswer): AnswerOutcome =
        when (answer) {
            is ChatAnswer.Hold -> held(answer)
            is ChatAnswer.LetThrough -> decisions.letThrough(answer.chat).outcome()
            is ChatAnswer.Dismiss -> decisions.askedAbout(answer.chats).outcome()
        }

    private suspend fun held(hold: ChatAnswer.Hold): AnswerOutcome =
        when (val update = settings.update { it.withRuleChanged(hold.rule, RuleChange.AddChat(hold.chat)) }) {
            SettingsUpdate.Saved -> {
                AnswerOutcome.Saved
            }

            is SettingsUpdate.DuplicateNames -> {
                AnswerOutcome.Failed(update.detail)
            }

            is SettingsUpdate.Failure -> {
                AnswerOutcome.Failed(update.detail)
            }
        }

    private fun ChatDecisionSave.outcome(): AnswerOutcome =
        when (this) {
            ChatDecisionSave.Saved -> AnswerOutcome.Saved
            is ChatDecisionSave.Failed -> AnswerOutcome.Failed(detail)
        }
}
