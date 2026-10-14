package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.Window
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

    fun spanDescription(): String =
        when (DailyHours.between(start, end)) {
            is DailyHours.SameDay -> "Same day"
            is DailyHours.Overnight -> "Runs past midnight into the next day"
            is DailyHours.FullDay -> "A full 24 hours"
        }

    fun toggle(day: DayOfWeek): WindowDraft = copy(days = days.toggled(day))
}
