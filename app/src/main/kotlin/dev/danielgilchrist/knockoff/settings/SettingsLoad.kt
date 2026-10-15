package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode

sealed interface SettingsLoad {
    sealed interface Usable : SettingsLoad {
        val settings: Settings
    }

    data class Loaded(
        override val settings: Settings,
    ) : Usable

    data object Empty : Usable {
        override val settings: Settings get() = Settings.DEFAULT
    }

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
