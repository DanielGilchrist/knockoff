package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice

data class Settings(
    val rules: RuleSet,
    val theme: Theme,
) {
    companion object {
        val DEFAULT: Settings
            get() =
                Settings(
                    RuleSet.of(
                        Rule(
                            name = RuleName.WORK,
                            state = RuleState.Enabled,
                            policy = Policy.DeliverOnlyDuring,
                            schedule = Schedule.of(Window.WORKING_HOURS),
                            targets = emptySet(),
                            zone = RuleZone.Default,
                        ),
                        ZoneChoice.Device,
                    ),
                    Theme.System,
                )

        fun fromJson(text: String): SettingsDecode =
            when (
                val decode =
                    VersionedJson.decode<StoredSettingsDocument>(
                        text,
                        mapOf(
                            StoredSettingsV1.VERSION to StoredSettingsV1.serializer(),
                            StoredSettingsV2.VERSION to StoredSettingsV2.serializer(),
                            StoredSettings.CURRENT_VERSION to StoredSettings.serializer(),
                        ),
                    )
            ) {
                is VersionedDecode.Decoded -> decode.value.toSettings()
                is VersionedDecode.Unreadable -> SettingsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> SettingsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSettings.from(this))
}
