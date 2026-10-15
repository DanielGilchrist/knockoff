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

data class Settings(
    val rules: RuleSet,
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
                        ),
                    ),
                )

        fun fromJson(text: String): SettingsDecode =
            when (
                val decode =
                    VersionedJson.decode(
                        text,
                        StoredSettings.CURRENT_VERSION,
                        StoredSettings.serializer(),
                    )
            ) {
                is VersionedDecode.Decoded -> decode.value.toSettings()
                is VersionedDecode.Unreadable -> SettingsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> SettingsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSettings.from(this))
}
