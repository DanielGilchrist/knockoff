package dev.danielgilchrist.knockoff.ui

sealed interface SaveStatus {
    data object Idle : SaveStatus

    data class Failed(
        val detail: String,
    ) : SaveStatus
}
