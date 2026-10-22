package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV6(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV6>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 6
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV7(StoredSettingsV7.VERSION, defaultZone, theme, rules.map(StoredRuleV6::upgraded))
            .toSettings()
}
