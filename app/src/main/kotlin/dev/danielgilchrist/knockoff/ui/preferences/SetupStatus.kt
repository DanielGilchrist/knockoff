package dev.danielgilchrist.knockoff.ui.preferences

sealed interface SetupStatus {
    data object Done : SetupStatus

    data class Needed(
        val label: String,
    ) : SetupStatus
}
