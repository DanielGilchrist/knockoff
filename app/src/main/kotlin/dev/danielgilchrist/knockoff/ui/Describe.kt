package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
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

internal fun Rule.describeTargets(): String =
    when (targets.size) {
        0 -> "No apps, accounts or chats yet, so this rule does nothing"
        1 -> "1 app, account or chat"
        else -> "${targets.size} apps, accounts or chats"
    }

internal fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.UK)

internal fun DraftProblem.describe(): String =
    when (this) {
        DraftProblem.BlankName -> "Give the rule a name."
        DraftProblem.DuplicateName -> "Another rule already has this name."
        DraftProblem.NoHours -> "Add at least one set of hours."
        DraftProblem.BreaksCoverAllHours -> "The breaks cover all of the hours, so the rule would never apply."
        is DraftProblem.HoursWithoutDays -> "Hours $number need at least one day."
        is DraftProblem.UnaddedAccount -> "The Gmail account being typed isn't valid: $detail"
        is DraftProblem.NotSaved -> "Couldn't save: $detail"
    }
