package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode

sealed interface SettingsFile {
    data object Absent : SettingsFile

    data class Loaded(
        val settings: Settings,
    ) : SettingsFile

    data class Corrupt(
        val failure: SettingsDecode.Failure,
    ) : SettingsFile
}
