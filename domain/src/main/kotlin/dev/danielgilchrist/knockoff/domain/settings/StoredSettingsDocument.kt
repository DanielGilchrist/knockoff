package dev.danielgilchrist.knockoff.domain.settings

internal sealed interface StoredSettingsDocument {
    fun toSettings(): SettingsDecode
}
