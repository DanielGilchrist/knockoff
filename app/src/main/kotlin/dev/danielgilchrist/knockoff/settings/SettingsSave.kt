package dev.danielgilchrist.knockoff.settings

sealed interface SettingsSave {
    data object Saved : SettingsSave

    data class WriteFailed(
        val detail: String,
    ) : SettingsSave
}
