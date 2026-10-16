package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV2(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV2>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 2
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV3(StoredSettingsV3.VERSION, defaultZone, theme, rules.map(StoredRuleV2::upgraded)).toSettings()
}
