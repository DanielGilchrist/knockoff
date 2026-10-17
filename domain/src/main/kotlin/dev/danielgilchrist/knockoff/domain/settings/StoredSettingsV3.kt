package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV3(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV3>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 3
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV4(StoredSettingsV4.VERSION, defaultZone, theme, rules.map(StoredRuleV3::upgraded)).toSettings()
}
