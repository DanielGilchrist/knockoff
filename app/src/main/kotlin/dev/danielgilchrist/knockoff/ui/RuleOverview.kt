package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.ui.zone.label

data class RuleOverview(
    val sentence: String,
    val effect: String,
    val apps: String,
    val hours: String,
) {
    companion object {
        fun of(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): RuleOverview {
            val effect = effect(rule, labelOf)

            return RuleOverview(
                sentence = sentence(rule, effect),
                effect = effect,
                apps = apps(rule, labelOf),
                hours = hours(rule),
            )
        }

        private fun sentence(
            rule: Rule,
            effect: String,
        ): String {
            val off =
                when (rule.state) {
                    RuleState.Enabled -> ""
                    RuleState.Disabled -> "Off. "
                }

            return off + effect
        }

        private fun effect(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): String {
            val held = HeldPhrases.of(rule, labelOf)
            val asks = asks(rule, labelOf)

            return when {
                rule.apps.scopes.isEmpty() -> {
                    "No apps yet. Add one under Apps."
                }

                held.isEmpty() -> {
                    "Nothing picked yet.$asks"
                }

                else -> {
                    HeldPhrases.effect(
                        HeldPhrases.listed(held.first(), held.drop(1)),
                        rule.policy,
                        rule.schedule.describeInSentence(),
                    ) + asks
                }
            }
        }

        private fun asks(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): String {
            val asked = rule.asking.map(labelOf)

            return when (rule.state) {
                RuleState.Disabled -> {
                    ""
                }

                RuleState.Enabled -> {
                    when {
                        asked.isEmpty() -> ""
                        else -> " You'll be asked about new ${HeldPhrases.listed(asked.first(), asked.drop(1))} chats."
                    }
                }
            }
        }

        private fun apps(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): String {
            val labels =
                rule.apps.scopes.keys
                    .map(labelOf)

            return when {
                labels.isEmpty() -> "No apps yet"
                else -> HeldPhrases.listed(labels.first(), labels.drop(1))
            }
        }

        private fun hours(rule: Rule): String {
            val windows = rule.schedule.windows.joinToString("; ") { it.describe() }
            val breaks = rule.schedule.breaks.map { it.name.value }

            val hours =
                when {
                    breaks.isEmpty() -> windows
                    else -> "$windows · except ${breaks.joinToString()}"
                }

            return when (val zone = rule.zone) {
                RuleZone.Default -> hours
                is RuleZone.Chosen -> "$hours · ${zone.choice.label()}"
            }
        }
    }
}
