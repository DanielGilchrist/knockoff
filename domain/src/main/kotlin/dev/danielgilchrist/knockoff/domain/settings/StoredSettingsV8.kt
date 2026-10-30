package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV8(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRuleV8>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 8
    }

    override fun toSettings(): SettingsDecode =
        StoredSettings(
            StoredSettings.CURRENT_VERSION,
            defaultZone,
            theme,
            StoredHolidayCalendar.None,
            rules.map(StoredRuleV8::upgraded),
        ).toSettings()
}
