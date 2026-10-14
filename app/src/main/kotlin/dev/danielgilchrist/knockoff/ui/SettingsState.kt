package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.settings.Settings

sealed interface SettingsState {
    data object Loading : SettingsState

    data class Ready(
        val settings: Settings,
        val origin: SettingsOrigin,
    ) : SettingsState

    data class Failed(
        val detail: String,
    ) : SettingsState
}
