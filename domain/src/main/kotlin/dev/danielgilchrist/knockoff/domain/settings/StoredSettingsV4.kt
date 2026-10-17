package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV4(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV4>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 4
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV5(StoredSettingsV5.VERSION, defaultZone, theme, rules.map(StoredRuleV4::upgraded)).toSettings()
}
