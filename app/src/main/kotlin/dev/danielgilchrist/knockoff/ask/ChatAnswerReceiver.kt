package dev.danielgilchrist.knockoff.ask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.danielgilchrist.knockoff.KnockoffApplication
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswerDecode
import kotlinx.coroutines.launch

class ChatAnswerReceiver : BroadcastReceiver() {
    companion object {
        private const val EXTRA_ANSWER = "dev.danielgilchrist.knockoff.extra.ANSWER"

        fun intent(
            context: Context,
            answer: ChatAnswer,
        ): Intent = Intent(context, ChatAnswerReceiver::class.java).putExtra(EXTRA_ANSWER, answer.toJson())
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val knockoff = context.applicationContext as KnockoffApplication
        val pending = goAsync()

        knockoff.scope.launch {
            try {
                when (val decode = ChatAnswer.fromJson(intent.getStringExtra(EXTRA_ANSWER).orEmpty())) {
                    is ChatAnswerDecode.Decoded -> report(knockoff.chatAnswers.answer(decode.answer))
                    is ChatAnswerDecode.Failed -> Log.w(LOG_TAG, "chat answer unreadable: ${decode.detail}")
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun report(outcome: AnswerOutcome) {
        when (outcome) {
            AnswerOutcome.Saved -> {}

            is AnswerOutcome.Failed -> {
                Log.w(LOG_TAG, "chat answer not saved: ${outcome.detail}")
            }
        }
    }
}
