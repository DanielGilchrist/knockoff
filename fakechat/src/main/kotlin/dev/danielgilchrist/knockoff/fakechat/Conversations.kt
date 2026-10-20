package dev.danielgilchrist.knockoff.fakechat

import android.content.Context

class Conversations(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("conversations", Context.MODE_PRIVATE)

    val chats: Map<String, List<String>>
        get() = recency().associateWith(::messages)

    fun messages(chat: String): List<String> = prefs.getString(MESSAGES + chat, null)?.split(SEPARATOR).orEmpty()

    fun add(
        chat: String,
        text: String,
    ) {
        prefs
            .edit()
            .putString(MESSAGES + chat, (messages(chat) + text).joinToString(SEPARATOR))
            .putString(RECENCY, (listOf(chat) + recency().minus(chat)).joinToString(SEPARATOR))
            .commit()
    }

    fun remove(chat: String) {
        prefs
            .edit()
            .remove(MESSAGES + chat)
            .putString(RECENCY, recency().minus(chat).joinToString(SEPARATOR))
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun recency(): List<String> =
        prefs
            .getString(RECENCY, null)
            ?.split(SEPARATOR)
            .orEmpty()
            .filter { it.isNotEmpty() }

    private companion object {
        const val MESSAGES = "messages."
        const val RECENCY = "recency"
        const val SEPARATOR = "\n"
    }
}
