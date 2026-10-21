package dev.danielgilchrist.knockoff.fakechat

import android.content.Intent

sealed interface Command {
    data class Message(
        val chat: String,
        val text: String,
    ) : Command

    data class Dismiss(
        val chat: String,
    ) : Command

    data class Mail(
        val account: String,
        val from: String,
        val subject: String,
    ) : Command

    data object Clear : Command

    data class Invalid(
        val detail: String,
    ) : Command

    companion object {
        fun of(intent: Intent): Command {
            val chat = intent.getStringExtra("chat")

            return when (intent.getStringExtra("do") ?: "message") {
                "message" -> chat?.let { Message(it, intent.getStringExtra("text") ?: "Hi") } ?: Invalid("no chat")
                "dismiss" -> chat?.let(::Dismiss) ?: Invalid("no chat")
                "mail" -> mail(intent)
                "clear" -> Clear
                else -> Invalid("unknown command ${intent.getStringExtra("do")}")
            }
        }

        private fun mail(intent: Intent): Command =
            when (val account = intent.getStringExtra("account")) {
                null -> Invalid("no account")
                else -> Mail(account, intent.getStringExtra("from") ?: "Someone", intent.getStringExtra("text") ?: "Hi")
            }
    }
}
