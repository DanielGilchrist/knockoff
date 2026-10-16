package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettingsV1(
    private val version: Int,
    private val rules: List<StoredRuleV1>,
) : StoredSettingsDocument {
    companion object {
        const val VERSION = 1
    }

    override fun toSettings(): SettingsDecode =
        StoredSettingsV2(
            StoredSettingsV2.VERSION,
            ZoneChoice.Device.stored(),
            Theme.System.name,
            rules.map(StoredRuleV1::upgraded),
        ).toSettings()
}
