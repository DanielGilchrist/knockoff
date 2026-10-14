package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode

sealed interface SettingsLoad {
    data class Loaded(
        val settings: Settings,
    ) : SettingsLoad

    data object Empty : SettingsLoad

    sealed interface Failure : SettingsLoad {
        val detail: String
    }

    data class ReadFailed(
        override val detail: String,
    ) : Failure

    data class Corrupt(
        val failure: SettingsDecode.Failure,
    ) : Failure {
        override val detail: String get() = failure.detail
    }
}
