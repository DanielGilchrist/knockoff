package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredSettings(
    private val version: Int,
    private val rules: List<StoredRule>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(settings: Settings): StoredSettings =
            StoredSettings(CURRENT_VERSION, settings.rules.rules.map(StoredRule::from))
    }

    fun toSettings(): SettingsDecode =
        when (val parsed = rules.parseEach("rule") { it.parse() }) {
            is Parsed.Failed -> {
                SettingsDecode.Invalid(parsed.detail)
            }

            is Parsed.Ok -> {
                when (val parse = RuleSet.parse(parsed.value)) {
                    is RuleSetParse.Valid -> {
                        SettingsDecode.Decoded(Settings(parse.rules))
                    }

                    is RuleSetParse.DuplicateNames -> {
                        SettingsDecode.Invalid(
                            "duplicate rule names ${parse.names.map { it.value }}",
                        )
                    }
                }
            }
        }
}
