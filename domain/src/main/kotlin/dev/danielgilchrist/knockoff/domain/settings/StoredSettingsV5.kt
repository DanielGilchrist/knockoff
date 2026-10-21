package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV5(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV5>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 5
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV6(StoredSettingsV6.VERSION, defaultZone, theme, rules.map(StoredRuleV5::upgraded))
            .toSettings()
}
