package dev.danielgilchrist.knockoff.chat

sealed interface ChatRecord {
    data object Recorded : ChatRecord

    data object Unchanged : ChatRecord

    data class Failed(
        val detail: String,
    ) : ChatRecord
}
