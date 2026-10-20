package dev.danielgilchrist.knockoff.fakechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val conversations = Conversations(context)
        val notifications = FakeNotifications(context)

        when (val command = Command.of(intent)) {
            is Command.Message -> {
                conversations.add(command.chat, command.text)
                notifications.chat(command.chat, conversations.messages(command.chat))
                notifications.summary(conversations.chats)
            }

            is Command.Dismiss -> {
                when {
                    command.chat in conversations.chats -> dismiss(command.chat, conversations, notifications)
                    else -> Log.w(TAG, "ignored: no chat named ${command.chat}")
                }
            }

            Command.Clear -> {
                conversations.clear()
                notifications.cancelAll()
            }

            is Command.Invalid -> {
                Log.w(TAG, "ignored: ${command.detail}")
            }
        }
    }

    private fun dismiss(
        chat: String,
        conversations: Conversations,
        notifications: FakeNotifications,
    ) {
        conversations.remove(chat)
        notifications.cancel(chat)

        val chats = conversations.chats

        when {
            chats.isEmpty() -> notifications.cancelSummary()
            else -> notifications.summary(chats)
        }
    }

    private companion object {
        const val TAG = "FakeChat"
    }
}
