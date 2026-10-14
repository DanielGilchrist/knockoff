package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.Window
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

data class Settings(
    val rules: RuleSet,
) {
    companion object {
        private const val VERSION = "version"

        private val json = Json { prettyPrint = true }

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

        fun fromJson(text: String): SettingsDecode {
            val root =
                try {
                    json.parseToJsonElement(text)
                } catch (exception: SerializationException) {
                    return SettingsDecode.Unreadable(exception.toString())
                }

            return when (root) {
                is JsonObject -> versioned(root)
                else -> SettingsDecode.Unreadable("not a JSON object")
            }
        }

        private fun versioned(document: JsonObject): SettingsDecode {
            val version =
                when (val element = document[VERSION]) {
                    is JsonPrimitive -> if (element.isString) null else element.intOrNull
                    else -> null
                }

            return when (version) {
                null -> SettingsDecode.Unreadable("missing or non-integer \"$VERSION\"")
                StoredSettings.CURRENT_VERSION -> current(document)
                else -> SettingsDecode.UnsupportedVersion(version)
            }
        }

        private fun current(document: JsonObject): SettingsDecode {
            val stored =
                try {
                    json.decodeFromJsonElement<StoredSettings>(document)
                } catch (exception: SerializationException) {
                    return SettingsDecode.Unreadable(exception.toString())
                }

            return stored.toSettings()
        }
    }

    fun toJson(): String = json.encodeToString(StoredSettings.from(this))
}
