package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.ui.components.TileNote
import dev.danielgilchrist.knockoff.ui.toggled
import java.time.DayOfWeek
import java.time.LocalTime

data class WindowDraft(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    companion object {
        val WORKING_HOURS: WindowDraft get() = from(Window.WORKING_HOURS)

        fun from(window: Window): WindowDraft = WindowDraft(window.days.days, window.hours.start, window.hours.end)
    }

    fun endNote(): TileNote =
        when (DailyHours.between(start, end)) {
            is DailyHours.SameDay -> TileNote.None
            is DailyHours.Overnight -> TileNote.Shown("Next day")
            is DailyHours.FullDay -> TileNote.Shown("24 hours later")
        }

    fun runsPastMidnight(): Boolean =
        when (DailyHours.between(start, end)) {
            is DailyHours.Overnight -> true
            is DailyHours.SameDay, is DailyHours.FullDay -> false
        }

    fun toggle(day: DayOfWeek): WindowDraft = copy(days = days.toggled(day))

    fun parse(withoutDays: DraftProblem.MissingDays): Checked<Window> =
        when (val parse = WeekDays.parse(days)) {
            is WeekDaysParse.Valid -> Checked.Ok(Window(parse.days, DailyHours.between(start, end)))
            WeekDaysParse.Empty -> Checked.Problems(listOf(withoutDays))
        }
}
