package dev.danielgilchrist.knockoff.ask

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.danielgilchrist.knockoff.MainActivity
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NotificationPermission

class ChatQuestionNotifier(
    private val context: Context,
) {
    private companion object {
        const val CHANNEL_ID = "new_chats"
        const val NOTIFICATION_ID = 1
        const val OPEN_REQUEST = 0
        const val LET_THROUGH_REQUEST = 1
        const val DISMISS_REQUEST = 2
        const val HOLD_REQUEST = 3
    }

    private val manager = context.getSystemService(NotificationManager::class.java)

    suspend fun show(
        prompt: ChatPrompt,
        permission: NotificationPermission,
    ) {
        when (prompt) {
            ChatPrompt.Unknown -> {}

            is ChatPrompt.Ready -> {
                when (val first = prompt.questions.firstOrNull()) {
                    null -> manager.cancel(NOTIFICATION_ID)
                    else -> post(first, prompt.questions.drop(1), permission)
                }
            }
        }
    }

    private suspend fun post(
        first: ChatQuestion,
        rest: List<ChatQuestion>,
        permission: NotificationPermission,
    ) {
        when (permission) {
            NotificationPermission.Granted -> {
                val view = QuestionView.of(first, rest, InstalledApps.load(context.packageManager)::labelOf)
                manager.notify(NOTIFICATION_ID, notification(view))
            }

            NotificationPermission.NotGranted -> {}
        }
    }

    private fun notification(view: QuestionView): Notification {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "New chats", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Asks whether new chats should join a rule"
            },
        )
        return Notification
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_new_chat)
            .setContentTitle(view.title)
            .setSubText("New chat")
            .setContentText(view.text)
            .setStyle(Notification.BigTextStyle().bigText(view.text))
            .setOnlyAlertOnce(true)
            .setContentIntent(opening())
            .setDeleteIntent(answering(DISMISS_REQUEST, view.dismiss))
            .setActions(
                *(
                    view.holds.mapIndexed { index, hold ->
                        action(AnswerLabels.addTo(hold.rule), answering(HOLD_REQUEST + index, hold))
                    } + action(AnswerLabels.LET_THROUGH, answering(LET_THROUGH_REQUEST, view.letThrough))
                ).toTypedArray(),
            ).build()
    }

    private fun action(
        title: String,
        intent: PendingIntent,
    ): Notification.Action = Notification.Action.Builder(null, title, intent).build()

    private fun opening(): PendingIntent =
        PendingIntent.getActivity(
            context,
            OPEN_REQUEST,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun answering(
        request: Int,
        answer: ChatAnswer,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            request,
            ChatAnswerReceiver.intent(context, answer),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
