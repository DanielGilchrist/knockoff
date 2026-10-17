package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.schedule.Break
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.ui.editor.DraftProblem
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun ZoneId.describe(): String {
    val offset = rules.getOffset(Instant.now()).id.replace("Z", "+00:00")

    return "${id.replace('_', ' ')} (UTC$offset)"
}

internal fun Policy.describe(): String =
    when (this) {
        Policy.DeliverOnlyDuring -> "Deliver only during these hours"
        Policy.HoldDuring -> "Hold during these hours"
    }

internal fun Window.describe(): String {
    val dayNames = DayOfWeek.entries.filter { it in days }.joinToString { it.shortName() }

    return "$dayNames ${hours.start}–${hours.end}"
}

internal fun Break.describe(): String = "Except ${name.value}: ${window.describe()}"

internal fun Rule.describeTargets(): String {
    val held =
        when (targets.size) {
            0 -> "Nothing held yet"
            1 -> "1 app, account, chat or pattern"
            else -> "${targets.size} apps, accounts, chats or patterns"
        }

    return when (asking.size) {
        0 -> if (targets.isEmpty()) "Nothing chosen yet, so this rule does nothing" else held
        1 -> "$held. Asks about new chats from 1 app"
        else -> "$held. Asks about new chats from ${asking.size} apps"
    }
}

internal fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.UK)

internal fun DraftProblem.describe(): String =
    when (this) {
        DraftProblem.BlankName -> "Give the rule a name."
        DraftProblem.DuplicateName -> "Another rule already has this name."
        DraftProblem.NoHours -> "Add at least one set of hours."
        DraftProblem.BreaksCoverAllHours -> "The breaks cover all of the hours, so the rule would never apply."
        is DraftProblem.HoursWithoutDays -> "Hours $number need at least one day."
        is DraftProblem.BlankBreakName -> "Give break $number a name."
        is DraftProblem.BreakWithoutDays -> "Break $number needs at least one day."
        is DraftProblem.BlankPattern -> "Pattern $number is empty."
        is DraftProblem.InvalidPattern -> "Pattern $number isn't a valid regular expression: $detail"
        is DraftProblem.UnaddedAccount -> "The Gmail account being typed isn't valid: $detail"
        is DraftProblem.NotSaved -> "Couldn't save: $detail"
    }

private const val COVERAGE_LIMIT = 5

internal fun describeCoverage(names: List<String>): String {
    val shown = names.take(COVERAGE_LIMIT).joinToString()
    val more =
        when {
            names.size > COVERAGE_LIMIT -> " and ${names.size - COVERAGE_LIMIT} more"
            else -> ""
        }

    return when {
        names.isEmpty() -> "Covers no chats seen so far."
        else -> "Seen chats it covers now: $shown$more"
    }
}
