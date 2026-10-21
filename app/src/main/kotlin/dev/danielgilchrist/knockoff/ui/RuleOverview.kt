package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice

data class RuleOverview(
    val sentence: String,
    val effect: String,
    val what: String,
    val hours: String,
    val advanced: String,
) {
    companion object {
        fun of(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): RuleOverview {
            val effect = effect(rule)

            return RuleOverview(
                sentence = sentence(rule, effect),
                effect = effect,
                what = what(rule, labelOf),
                hours = hours(rule),
                advanced = "${zone(rule.zone)} · ${counted(patterns(rule), "pattern")}",
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
            val breaks = rule.schedule.breaks.map { it.name.value }
            val hours =
                rule.schedule.windows.joinToString(" and ") { it.describeInSentence() } +
                    breaks.joinToString(prefix = ", except ").takeIf { breaks.isNotEmpty() }.orEmpty()

            return when {
                held.isNotEmpty() && rule.policy == Policy.DeliverOnlyDuring -> {
                    "Lets notifications from ${held.listed()} through only $hours."
                }

                held.isNotEmpty() -> {
                    "Holds notifications from ${held.listed()} $hours."
                }

                rule.asking.isNotEmpty() -> {
                    "Holds nothing yet. Asks about new chats from ${counted(rule.asking.size, "app")}."
                }

                else -> {
                    "Holds nothing yet. Choose apps or chats under What."
                }
            }
        }

        private fun held(rule: Rule): List<String> {
            val targets = rule.targets

            return listOf(
                targets.count { it is Target.App } to "app",
                targets.count { it is Target.Account } to "Gmail account",
                targets.count { it is Target.Chat } to "chat",
                patterns(rule) to "pattern",
            ).filter { (count, _) -> count > 0 }.map { (count, noun) -> counted(count, noun) }
        }

        private fun what(
            rule: Rule,
            labelOf: (PackageName) -> String,
        ): String {
            val parts =
                rule.targets
                    .flatMap { target ->
                        when (target) {
                            is Target.App -> listOf(labelOf(target.packageName))
                            is Target.Account -> listOf("Gmail (${target.label.value})")
                            is Target.Chat, is Target.Pattern -> emptyList()
                        }
                    }.sorted() +
                    listOf(counted(rule.chats.size, "chat")).filter { rule.chats.isNotEmpty() } +
                    listOf(counted(patterns(rule), "pattern")).filter { patterns(rule) > 0 } +
                    rule.asking.map { "asks about new ${labelOf(it)} chats" }.sorted()

            return parts.joinToString(" · ").ifEmpty { "Nothing chosen" }
        }

        private fun hours(rule: Rule): String {
            val windows = rule.schedule.windows.joinToString("; ") { it.describe() }
            val breaks = rule.schedule.breaks.map { it.name.value }

            return when {
                breaks.isEmpty() -> windows
                else -> "$windows · except ${breaks.joinToString()}"
            }
        }

        private fun patterns(rule: Rule): Int = rule.targets.count { it is Target.Pattern }

        private fun zone(zone: RuleZone): String =
            when (zone) {
                RuleZone.Default -> {
                    "App default time zone"
                }

                is RuleZone.Chosen -> {
                    when (val choice = zone.choice) {
                        ZoneChoice.Device -> "Device time zone"
                        is ZoneChoice.Fixed -> choice.zone.describe()
                    }
                }
            }

        private fun counted(
            count: Int,
            noun: String,
        ): String =
            when (count) {
                0 -> "No ${noun}s"
                1 -> "1 $noun"
                else -> "$count ${noun}s"
            }

        private fun List<String>.listed(): String =
            when (size) {
                1 -> single()
                else -> "${dropLast(1).joinToString()} and ${last()}"
            }
    }
}
