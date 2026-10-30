package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV7(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV7>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 7
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV8(StoredSettingsV8.VERSION, defaultZone, theme, rules.map(StoredRuleV7::upgraded))
            .toSettings()
}
