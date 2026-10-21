package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber

data class QuestionView(
    val title: String,
    val text: String,
    val holds: List<ChatAnswer.Hold>,
    val letThrough: ChatAnswer.LetThrough,
    val dismiss: ChatAnswer.Dismiss,
) {
    companion object {
        private const val HOLD_ACTIONS = 2

        fun of(
            first: ChatQuestion,
            rest: List<ChatQuestion>,
            labelOf: (PackageName) -> String,
        ): QuestionView {
            val chat = first.chat
            val waiting =
                when (rest.size) {
                    0 -> ""
                    1 -> " 1 more is waiting."
                    else -> " ${rest.size} more are waiting."
                }

            return QuestionView(
                title = chat.name.value,
                text = "From ${labelOf(
                    chat.packageName,
                )}. ${AnswerLabels.addOrLetThrough(GrammaticalNumber.One)}.$waiting",
                holds = first.rules.take(HOLD_ACTIONS).map { ChatAnswer.Hold(it, chat) },
                letThrough = ChatAnswer.LetThrough(chat.key),
                dismiss = ChatAnswer.Dismiss((listOf(first) + rest).map { it.chat.key }.toSet()),
            )
        }
    }
}
