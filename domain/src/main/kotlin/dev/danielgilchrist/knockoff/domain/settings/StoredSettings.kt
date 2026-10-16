package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoiceParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettings(
    private val version: Int,
    private val defaultZone: String,
    private val theme: String,
    private val rules: List<StoredRule>,
) : StoredSettingsDocument {
    companion object {
        const val CURRENT_VERSION = 3

        fun from(settings: Settings): StoredSettings =
            StoredSettings(
                version = CURRENT_VERSION,
                defaultZone = settings.rules.defaultZone.stored(),
                theme = settings.theme.name,
                rules = settings.rules.rules.map(StoredRule::from),
            )

        private fun settingsOf(
            rules: List<StoredRule>,
            defaultZone: ZoneChoice,
            theme: Theme,
        ): SettingsDecode =
            when (val parsed = rules.parseEach("rule") { it.parse() }) {
                is Parsed.Failed -> {
                    SettingsDecode.Invalid(parsed.detail)
                }

                is Parsed.Ok -> {
                    when (val parse = RuleSet.parse(parsed.value, defaultZone)) {
                        is RuleSetParse.Valid -> {
                            SettingsDecode.Decoded(Settings(parse.rules, theme))
                        }

                        is RuleSetParse.DuplicateNames -> {
                            SettingsDecode.Invalid("duplicate rule names ${parse.names.map { it.value }}")
                        }
                    }
                }
            }
    }

    override fun toSettings(): SettingsDecode =
        when (val zone = ZoneChoice.parse(defaultZone)) {
            is ZoneChoiceParse.Unknown -> {
                SettingsDecode.Invalid("unknown default zone \"${zone.raw}\": ${zone.detail}")
            }

            is ZoneChoiceParse.Valid -> {
                when (val known = Theme.entries.firstOrNull { it.name == theme }) {
                    null -> SettingsDecode.Invalid("unknown theme \"$theme\"")
                    else -> settingsOf(rules, zone.choice, known)
                }
            }
        }
}
