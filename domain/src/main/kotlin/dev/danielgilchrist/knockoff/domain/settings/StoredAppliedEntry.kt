package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoiceParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredAppliedEntry(
    private val settings: String,
    private val zone: String,
) {
    companion object {
        fun from(applied: AppliedRules): StoredAppliedEntry =
            StoredAppliedEntry(Settings(applied.rules, Theme.System).toJson(), applied.zone.id)
    }

    fun toAppliedRules(): Parsed<AppliedRules> =
        when (val decode = Settings.fromJson(settings)) {
            is SettingsDecode.Failure -> {
                Parsed.Failed(decode.detail)
            }

            is SettingsDecode.Decoded -> {
                when (val parse = ZoneChoice.parse(zone)) {
                    is ZoneChoiceParse.Unknown -> {
                        Parsed.Failed("unknown zone ${parse.raw}: ${parse.detail}")
                    }

                    is ZoneChoiceParse.Valid -> {
                        when (val choice = parse.choice) {
                            ZoneChoice.Device -> {
                                Parsed.Failed("applied zone is not a fixed zone")
                            }

                            is ZoneChoice.Fixed -> {
                                Parsed.Ok(AppliedRules(decode.settings.rules, choice.zone))
                            }
                        }
                    }
                }
            }
        }
}
