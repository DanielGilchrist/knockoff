package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
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
    val holidayCalendar: HolidayCalendar,
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
                            apps = RuleApps.NONE,
                            zone = RuleZone.Default,
                            daysOff = DaysOff.NONE,
                        ),
                        ZoneChoice.Device,
                    ),
                    Theme.System,
                    HolidayCalendar.None,
                )

        fun fromJson(text: String): SettingsDecode =
            when (
                val decode =
                    VersionedJson.decode<StoredSettingsDocument>(
                        text,
                        mapOf(
                            StoredSettingsV1.VERSION to StoredSettingsV1.serializer(),
                            StoredSettingsV2.VERSION to StoredSettingsV2.serializer(),
                            StoredSettingsV3.VERSION to StoredSettingsV3.serializer(),
                            StoredSettingsV4.VERSION to StoredSettingsV4.serializer(),
                            StoredSettingsV5.VERSION to StoredSettingsV5.serializer(),
                            StoredSettingsV6.VERSION to StoredSettingsV6.serializer(),
                            StoredSettingsV7.VERSION to StoredSettingsV7.serializer(),
                            StoredSettingsV8.VERSION to StoredSettingsV8.serializer(),
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
