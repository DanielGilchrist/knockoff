package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.ui.zone.label

data class RuleOverview(
    val sentence: String,
    val effect: String,
    val apps: String,
    val hours: String,
) {
    companion object {
        private const val SHOWN_APPS = 3

        fun of(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): RuleOverview {
            val effect = effect(rule)

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

        private fun effect(rule: Rule): String {
            val held = held(rule)
            val hours = rule.schedule.describeInSentence()

            return when {
                held.isNotEmpty() && rule.policy == Policy.DeliverOnlyDuring -> {
                    "Lets notifications from ${held.listed()} through only $hours."
                }

                held.isNotEmpty() -> {
                    "Holds notifications from ${held.listed()} $hours."
                }

                rule.asking.isNotEmpty() -> {
                    "Holds nothing yet. Asks about new chats from ${counted(rule.asking.size, "app", "apps")}."
                }

                else -> {
                    "Holds nothing yet. Add an app under Apps."
                }
            }
        }

        private fun held(rule: Rule): List<String> {
            val targets = rule.targets

            return listOf(
                targets.count { it is Target.App } to "app",
                accounts(rule, GroupNoun.Accounts) to GroupNoun.Accounts.one,
                accounts(rule, GroupNoun.Groups) to GroupNoun.Groups.one,
                targets.count { it is Target.Chat } to "chat",
                patterns(rule) to "pattern",
            ).filter { (count, _) -> count > 0 }.map { (count, noun) -> counted(count, noun, "${noun}s") }
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
                labels.size <= SHOWN_APPS -> labels.joinToString()
                else -> "${labels.take(SHOWN_APPS).joinToString()} and ${labels.size - SHOWN_APPS} more"
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

        private fun accounts(
            rule: Rule,
            noun: GroupNoun,
        ): Int =
            rule.apps.scopes.values.sumOf { scope ->
                when (scope) {
                    AppScope.Everything -> {
                        0
                    }

                    is AppScope.Narrowed -> {
                        when (GroupNoun.of(scope.accounts.values)) {
                            noun -> scope.accounts.size
                            else -> 0
                        }
                    }
                }
            }

        private fun patterns(rule: Rule): Int = rule.targets.count { it is Target.Pattern }

        private fun List<String>.listed(): String =
            when (size) {
                1 -> single()
                else -> "${dropLast(1).joinToString()} and ${last()}"
            }
    }
}
