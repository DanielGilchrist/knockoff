package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.holiday.HolidayRead
import dev.danielgilchrist.knockoff.holiday.HolidaysState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
private val DAY = DateTimeFormatter.ofPattern("d", Locale.UK)
private val WEEKDAY_DAY_MONTH = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
private const val SHOWN_LEAVE = 3

internal fun Leave.describe(today: LocalDate): String =
    when {
        first == last && first == today -> "Today"
        first == last -> first.format(DAY_MONTH)
        first.month == last.month && first.year == last.year -> "${first.format(DAY)}–${last.format(DAY_MONTH)}"
        else -> "${first.format(DAY_MONTH)} – ${last.format(DAY_MONTH)}"
    }

internal fun Leave.describeSpan(): String =
    when (first) {
        last -> first.format(WEEKDAY_DAY_MONTH)
        else -> counted(dates.size, "day", "days")
    }

internal fun DaysOff.describe(today: LocalDate): String {
    val leave = upcoming(today)
    val shown = leave.take(SHOWN_LEAVE).map { it.describe(today) }
    val more =
        when {
            leave.size > SHOWN_LEAVE -> listOf("${leave.size - SHOWN_LEAVE} more")
            else -> emptyList()
        }
    val holidays =
        when (publicHolidays) {
            PublicHolidays.Off -> listOf("Public holidays")
            PublicHolidays.AsUsual -> emptyList()
        }
    val parts = shown + more + holidays

    return when {
        parts.isEmpty() -> "None"
        else -> parts.joinToString(" · ")
    }
}

internal fun Policy.dayOffEffect(apps: String): String =
    when (this) {
        Policy.DeliverOnlyDuring -> "Holding $apps all day."
        Policy.HoldDuring -> "Letting $apps through all day."
    }

internal fun Policy.takeTodayOff(): String =
    when (this) {
        Policy.DeliverOnlyDuring -> "Take today off"
        Policy.HoldDuring -> "Pause for today"
    }

internal fun Policy.takeTodayOffEffect(apps: String): String =
    when (this) {
        Policy.DeliverOnlyDuring -> "Hold $apps until midnight"
        Policy.HoldDuring -> "Let $apps through until midnight"
    }

internal fun Policy.daysOffCaption(): String =
    when (this) {
        Policy.DeliverOnlyDuring -> "On a day off, this rule holds its apps from midnight to midnight."
        Policy.HoldDuring -> "On a day off, this rule lets its apps through from midnight to midnight."
    } + " The next day is as usual."

internal fun summaryOn(
    day: DayOff,
    policy: Policy,
    overview: RuleOverview,
): String =
    when (day) {
        DayOff.Scheduled -> overview.sentence
        DayOff.OnLeave -> "Day off today. ${policy.dayOffEffect(overview.apps)}"
        DayOff.PublicHoliday -> "Public holiday today. ${policy.dayOffEffect(overview.apps)}"
    }

internal fun HolidaysState.describeCalendar(): String =
    when (this) {
        HolidaysState.Loading -> {
            "Reading…"
        }

        HolidaysState.NotChosen -> {
            "Not set up"
        }

        is HolidaysState.Chosen -> {
            when (read) {
                is HolidayRead.Read -> {
                    (listOf(read.calendar.name) + regionNames(calendar)).joinToString(" · ")
                }

                HolidayRead.NoPermission -> {
                    "Needs calendar access"
                }

                HolidayRead.Missing -> {
                    "Calendar no longer on this phone"
                }

                is HolidayRead.Failed -> {
                    "Couldn't be read"
                }
            }
        }
    }

internal fun HolidaysState.describeSource(): String =
    when (this) {
        HolidaysState.Loading -> {
            "Reading the holiday calendar…"
        }

        HolidaysState.NotChosen -> {
            "No holiday calendar yet. Set one up in Settings › Public holidays."
        }

        is HolidaysState.Chosen -> {
            when (read) {
                is HolidayRead.Read -> {
                    val regions = regionNames(calendar)

                    when {
                        regions.isEmpty() -> "From ${read.calendar.name}"
                        else -> "From ${read.calendar.name}, with ${regions.joinToString()}"
                    }
                }

                HolidayRead.NoPermission -> {
                    "Knockoff can't read calendars. Allow it in Settings › Public holidays."
                }

                HolidayRead.Missing -> {
                    "The holiday calendar is no longer on this phone. Choose another in Settings."
                }

                is HolidayRead.Failed -> {
                    "The holiday calendar couldn't be read."
                }
            }
        }
    }

private fun regionNames(calendar: HolidayCalendar.Chosen): List<String> = calendar.regions.map { it.value }.sorted()
