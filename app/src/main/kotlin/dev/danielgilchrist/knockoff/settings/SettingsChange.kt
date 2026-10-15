package dev.danielgilchrist.knockoff.settings

sealed interface SettingsChange {
    data object Saved : SettingsChange

    data class Failed(
        val detail: String,
    ) : SettingsChange
}
