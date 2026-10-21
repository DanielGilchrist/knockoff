package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
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

private const val EVERY_DAY = "Every day"
private const val WEEKDAYS = "Weekdays"
private const val WEEKENDS = "Weekends"

internal fun Window.describe(): String = "${describeDays()} ${hours.start}–${hours.end}"

internal fun Window.describeInSentence(): String {
    val hours = "${hours.start}–${hours.end}"

    return when (val days = describeDays()) {
        EVERY_DAY -> "every day $hours"
        WEEKDAYS, WEEKENDS -> "on ${days.lowercase()} $hours"
        else -> "on $days $hours"
    }
}

internal fun Schedule.describeInSentence(): String {
    val names = breaks.map { it.name.value }

    return windows.joinToString(" and ") { it.describeInSentence() } +
        names.joinToString(prefix = ", except ").takeIf { names.isNotEmpty() }.orEmpty()
}

private fun Window.describeDays(): String {
    val chosen = DayOfWeek.entries.filter { it in days }.toSet()

    return when (chosen) {
        DayOfWeek.entries.toSet() -> EVERY_DAY
        DayOfWeek.entries.toSet() - setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> WEEKDAYS
        setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> WEEKENDS
        else -> chosen.joinToString { it.shortName() }
    }
}

internal fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.UK)

internal fun DraftProblem.describe(
    labelOf: (PackageName) -> String,
    pickOf: (PackageName) -> String,
): String =
    when (this) {
        DraftProblem.BlankName -> {
            "Give the rule a name."
        }

        DraftProblem.DuplicateName -> {
            "Another rule already has this name."
        }

        DraftProblem.NoHours -> {
            "Add at least one set of hours."
        }

        DraftProblem.BreaksCoverAllHours -> {
            "The breaks cover all of the hours, so the rule would never apply."
        }

        is DraftProblem.HoursWithoutDays -> {
            "Hours $number need at least one day."
        }

        is DraftProblem.BlankBreakName -> {
            "Give break $number a name."
        }

        is DraftProblem.BreakWithoutDays -> {
            "Break $number needs at least one day."
        }

        is DraftProblem.BlankPattern -> {
            "${labelOf(app)} pattern $number is empty."
        }

        is DraftProblem.InvalidPattern -> {
            "${labelOf(app)} pattern $number isn't a valid regular expression: $detail"
        }

        is DraftProblem.NothingChosen -> {
            "Pick at least one ${pickOf(app)} for ${labelOf(app)}, or choose All notifications."
        }

        is DraftProblem.NotSaved -> {
            "Couldn't save: $detail"
        }
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
